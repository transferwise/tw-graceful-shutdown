package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Slf4j
public class GracefulShutdowner implements ApplicationListener<ApplicationReadyEvent>, SmartLifecycle {

  @Autowired
  private GracefulShutdownProperties properties;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;

  @Autowired(required = false)
  private DefaultLifecycleProcessor defaultLifecycleProcessor;

  private boolean started;
  private AtomicBoolean isShuttingDown = new AtomicBoolean(false);

  @PostConstruct
  public void init() {
    log.info("Initialized graceful shutdown with timeout {} ms. Client reaction timeout will be {} ms.",
        properties.getShutdownTimeoutMs(), properties.getClientsReactionTimeMs());
    if (defaultLifecycleProcessor != null) {
      defaultLifecycleProcessor.setTimeoutPerShutdownPhase(2 * (properties.getClientsReactionTimeMs() + properties.getShutdownTimeoutMs()));
    }
  }

  @Override
  public void start() {
    started = true;
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }

  @Override
  public void stop() {
    if (isShuttingDown.compareAndSet(false, true)) { // Avoid executing stop() twice if both SmartLifecycle and EventListener gets triggered

      ExceptionUtils.doUnchecked(() -> {
        log.info("Graceful shutdown initiated.");

        List<GracefulShutdownStrategy> strategies = new ArrayList<>(gracefulShutdownStrategiesRegistry.getStrategies());
        Collections.reverse(strategies);

        strategies.forEach((s) -> {
          try {
            s.prepareForShutdown();
          } catch (Throwable t) {
            log.error(t.getMessage(), t);
          }
        });

        log.info("Waiting for " + properties.getClientsReactionTimeMs() + " ms for clients to understand this node should not be called anymore.");
        Thread.sleep(properties.getClientsReactionTimeMs());

        long start = System.currentTimeMillis();
        List<GracefulShutdownStrategy> redLightStrategies = new ArrayList<>(strategies);

        // slightly increase timeout here
        // now strategies could safely try to shut down within GracefulShutdownProperties.getShutdownTimeoutMs()
        int safeShutdownTimeoutMs = properties.getShutdownTimeoutMs() + 500;
        while (System.currentTimeMillis() - start < safeShutdownTimeoutMs) {
          redLightStrategies = redLightStrategies.stream().filter((s) -> {
            try {
              return !s.canShutdown();
            } catch (Throwable t) {
              log.error(t.getMessage(), t);
              return true;
            }
          }).collect(Collectors.toList());
          if (redLightStrategies.isEmpty()) {
            log.info("All strategies gave a green light for shutdown.");
            break;
          }
          log.info("Not shutting down yet, {} strategies have red light. Waiting for {} ms for next check.",
              redLightStrategies, properties.getStrategiesCheckIntervalTimeMs());
          Thread.sleep(properties.getStrategiesCheckIntervalTimeMs());
        }

        strategies.forEach((s) -> {
          try {
            s.applicationTerminating();
          } catch (Throwable t) {
            log.error(t.getMessage(), t);
          }
        });

        log.info("Shutting down.");
        started = false;
      });
    } else {
      log.info("Already shutting down...");
    }
  }

  @Override
  public boolean isRunning() {
    return started;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  @EventListener(ContextClosedEvent.class)
  public void onApplicationEvent(ContextClosedEvent event) {
    if (event.getApplicationContext() == applicationContext) {
      stop();
    }
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (event.getApplicationContext() == applicationContext) {
      started = true;
      try {
        gracefulShutdownStrategiesRegistry.getStrategies().forEach(GracefulShutdownStrategy::applicationStarted);
      } catch (Throwable t) {
        log.error(t.getMessage(), t);
        SpringApplication.exit(applicationContext, () -> 1);
      }
    }
  }
}
