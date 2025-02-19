package com.transferwise.common.gracefulshutdown;

import com.google.common.collect.ImmutableSet;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import sun.misc.Signal;

@Slf4j
public class GracefulShutdowner implements SmartLifecycle, InitializingBean {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private GracefulShutdownProperties properties;

  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;

  @Autowired
  private DefaultLifecycleProcessor defaultLifecycleProcessor;

  private volatile boolean running;

  @Override
  public void afterPropertiesSet() {
    log.info("Initialized graceful shutdown with timeout {} ms, client reaction timeout {} ms"
            + " and strategies '{}'.",
        properties.getShutdownTimeoutMs(),
        properties.getClientsReactionTimeMs(),
        gracefulShutdownStrategiesRegistry.getStrategies());

    defaultLifecycleProcessor.setTimeoutPerShutdownPhase(
        2L * (properties.getClientsReactionTimeMs() + properties.getShutdownTimeoutMs()));
  }

  @Override
  public void start() {
    running = true;
    initShutdownSignalLogging(new Signal("INT"), new Signal("TERM"));

    validateStrategies();

    log.info("Notifying all strategies that the application has started.");
    var strategies = gracefulShutdownStrategiesRegistry.getStrategies();
    for (var strategy : strategies) {
      try {
        strategy.applicationStarted();
        log.debug("'applicationStarted' hook called for strategy '{}'", strategy);
      } catch (Exception e) {
        throw new IllegalStateException("'applicationStarted' hook failed for strategy '" + strategy + "'.", e);
      }
    }
  }

  protected void initShutdownSignalLogging(Signal... signals) {
    for (var signal : signals) {
      log.info("Registering shutdown signal handler for signal '{}'", signal.toString());
      Signal.handle(signal, sig -> log.info("Received signal {}", sig.toString()));
    }
  }

  protected void validateStrategies() {
    var registeredStrategies = ImmutableSet.copyOf(gracefulShutdownStrategiesRegistry.getStrategies());
    var strategiesInAppContext = ImmutableSet.copyOf(applicationContext.getBeansOfType(GracefulShutdownStrategy.class).values());

    if (!registeredStrategies.equals(strategiesInAppContext)) {
      log.error(
          "Graceful shutdown strategies have changed between application initialization and runtime. This means, you have some kind of messed up "
              + "beans configuration and you should fix it. Strategies before: {}, strategies after: {}.",
          registeredStrategies, strategiesInAppContext);

      if (gracefulShutdownStrategiesRegistry instanceof DefaultGracefulShutdownStrategiesRegistry) {
        log.info("Replacing strategies with what we have now in app context.");
        var strategiesInAppContextList = new ArrayList<>(strategiesInAppContext);
        AnnotationAwareOrderComparator.sort(strategiesInAppContextList);
        ((DefaultGracefulShutdownStrategiesRegistry) gracefulShutdownStrategiesRegistry).setStrategies(strategiesInAppContextList);
      }
    }
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }

  @Override
  public void stop() {
    this.running = false;

    long startTimeMs = System.currentTimeMillis();
    try {
      log.info("Graceful shutdown initiated.");

      var strategies = new ArrayList<>(gracefulShutdownStrategiesRegistry.getStrategies());
      Collections.reverse(strategies);

      for (var strategy : strategies) {
        try {
          strategy.prepareForShutdown();
          log.debug("'prepareForShutdown' hook called for strategy '{}'", strategy);
        } catch (Throwable t) {
          log.error("Stopping strategy '{}' failed.", strategy, t);
        }
      }

      log.info("Waiting for " + properties.getClientsReactionTimeMs() + " ms for clients to understand"
          + " this node should not be called anymore.");
      Thread.sleep(properties.getClientsReactionTimeMs());

      for (var strategy : strategies) {
        try {
          strategy.clientReactionTimePassed();
          log.debug("'clientReactionTimePassed' hook called for strategy '{}'", strategy);
        } catch (Throwable t) {
          log.error("Notifying strategy '{}' about client reaction time passing failed.", strategy, t);
        }
      }

      long canShutdownLoopStartTimeMs = System.currentTimeMillis();
      List<GracefulShutdownStrategy> redLightStrategies = new ArrayList<>(strategies);

      while (System.currentTimeMillis() - canShutdownLoopStartTimeMs < properties.getShutdownTimeoutMs()) {
        redLightStrategies = redLightStrategies.stream().filter(strategy -> {
          try {
            return !strategy.canShutdown();
          } catch (Throwable t) {
            log.error("Checking if strategy '{}' can shutdown, failed.", strategy, t);
            return true;
          }
        }).collect(Collectors.toList());

        if (redLightStrategies.isEmpty()) {
          log.info("All strategies gave a green light for shutdown.");
          break;
        }
        log.info("Not shutting down yet, '{}' strategies have red light. Waiting for {} ms for next check.",
            redLightStrategies, properties.getStrategiesCheckIntervalTimeMs());

        Thread.sleep(properties.getStrategiesCheckIntervalTimeMs());
      }

      for (var strategy : strategies) {
        try {
          strategy.applicationTerminating();
          log.debug("'applicationTerminating' hook called for strategy '{}'", strategy);
        } catch (Throwable t) {
          log.error("Notifying strategy '{}' about termination, failed.", strategy, t);
        }
      }

      log.info("Graceful shutdown routine finished in {} ms.", System.currentTimeMillis() - startTimeMs);
    } catch (Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.error("Graceful shutdown failed in {} ms.", System.currentTimeMillis() - startTimeMs, t);
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  /**
   * Start as last, stop as first.
   */
  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

}
