package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.utils.ExecutorShutdownUtils;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class TaskSchedulersGracefulShutdownStrategyTest {

  private static final Duration checkMaxWaitTime = Duration.ofSeconds(25);

  private static final GracefulShutdownProperties gracefulShutdownProperties;

  static {
    gracefulShutdownProperties = new GracefulShutdownProperties();
    gracefulShutdownProperties.setShutdownTimeoutMs((int) Duration.ofSeconds(5).toMillis());
    gracefulShutdownProperties.setClientsReactionTimeMs(100);
  }

  @Test
  public void shutdown_invoked_on_application_context_classes() {
    // GIVEN
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    TaskSchedulersGracefulShutdownStrategy strategy = new TaskSchedulersGracefulShutdownStrategy(
        applicationContext,
        gracefulShutdownProperties
    );
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.initialize();
    ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("test", threadPoolTaskScheduler);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> ExecutorShutdownUtils.isTerminated(threadPoolTaskScheduler));
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
  }

  @Test
  public void shutdown_invoked_on_external_added_classes() {
    // GIVEN
    TaskSchedulersGracefulShutdownStrategy strategy = new TaskSchedulersGracefulShutdownStrategy(
        new StaticApplicationContext(),
        gracefulShutdownProperties
    );
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.initialize();
    strategy.addResource(threadPoolTaskScheduler);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> ExecutorShutdownUtils.isTerminated(threadPoolTaskScheduler));
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
  }

  @Test
  public void shutdown_timeout_is_applied_and_called_shutdownNow() {
    // GIVEN
    AtomicBoolean isInterrupted = new AtomicBoolean(false);

    TaskSchedulersGracefulShutdownStrategy strategy = new TaskSchedulersGracefulShutdownStrategy(
        new StaticApplicationContext(),
        gracefulShutdownProperties
    );
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.initialize();
    strategy.addResource(threadPoolTaskScheduler);
    threadPoolTaskScheduler.execute(() -> {
      try {
        Thread.sleep(checkMaxWaitTime.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        isInterrupted.set(true);
      }
    });

    // wait till task is started before requesting shutdown
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> threadPoolTaskScheduler.getActiveCount() == 1);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> ExecutorShutdownUtils.isTerminated(threadPoolTaskScheduler));
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
    Assertions.assertTrue(isInterrupted.get());
  }
}