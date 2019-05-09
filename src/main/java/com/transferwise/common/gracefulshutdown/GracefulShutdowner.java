package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class GracefulShutdowner implements ApplicationListener<ApplicationReadyEvent>, SmartLifecycle {
    @Autowired
    private GracefulShutdownProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DefaultLifecycleProcessor defaultLifecycleProcessor;

    private boolean started;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    protected List<GracefulShutdownStrategy> strategies;

    @PostConstruct
    public void init() {
        log.info("Initialized graceful shutdown with timeout " + properties.getShutdownTimeoutMs() + " ms. Client reaction timeout will be " + properties.getClientsReactionTimeMs() + " ms.");
        if (defaultLifecycleProcessor != null) {
            defaultLifecycleProcessor.setTimeoutPerShutdownPhase(2 * (properties.getClientsReactionTimeMs() + properties.getShutdownTimeoutMs()));
        }
    }

    protected List<GracefulShutdownStrategy> getStrategies() {
        if (strategies == null) {
            strategies = new ArrayList<>(applicationContext.getBeansOfType(GracefulShutdownStrategy.class).values());
            log.info("Following strategies were detected: '" + strategies + "'.");
        }
        return strategies;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        if (isShuttingDown.get()) {
            // Avoid calling stop() twice if both SmartLifecycle and EventListener gets triggered
            log.info("Already shutting down...");
            return;
        }

        isShuttingDown.set(true);

        ExceptionUtils.doUnchecked(() -> {
            log.info("Graceful shutdown initiated.");

            getStrategies().forEach((s) -> {
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
            while (System.currentTimeMillis() - start < properties.getShutdownTimeoutMs()) {
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
                log.info("Not shutting down yet, " + redLightStrategies + " strategies have red light. Waiting for " + properties.getStrategiesCheckIntervalTimeMs() + " ms for next check.");
                Thread.sleep(properties.getStrategiesCheckIntervalTimeMs());
            }

            getStrategies().forEach((s) -> {
                try {
                    s.applicationTerminating();
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            });

            log.info("Shutting down.");
            started = false;
        });
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
                getStrategies().forEach((s) -> {
                    s.applicationStarted();
                });
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                SpringApplication.exit(applicationContext, (ExitCodeGenerator) () -> 1);
            }
        }
    }
}
