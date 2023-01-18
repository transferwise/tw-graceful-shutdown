package com.transferwise.common.gracefulshutdown.strategies;

import java.util.concurrent.Executors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

class TaskSchedulersGracefulShutdownStrategyTest {

  @Test
  public void shutdown_invoked_on_private_classes() {
    // GIVEN
    var strategy = new TaskSchedulersGracefulShutdownStrategy();
    strategy = strategy.toBuilder()
            .applicationContext(new StaticApplicationContext())
            .build();

    var executor = Executors.newSingleThreadScheduledExecutor();
    var scheduler = new ConcurrentTaskScheduler(executor);
    strategy.addTaskScheduler(scheduler);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().until(executor::isShutdown);
  }
}