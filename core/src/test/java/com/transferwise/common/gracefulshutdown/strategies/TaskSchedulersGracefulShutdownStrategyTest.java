package com.transferwise.common.gracefulshutdown.strategies;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

class TaskSchedulersGracefulShutdownStrategyTest {

  @Test
  public void shutdown_invoked_on_private_classes() throws NoSuchFieldException, IllegalAccessException {
    // GIVEN
    var executor = Executors.newSingleThreadScheduledExecutor();
    var scheduler = new ConcurrentTaskScheduler(executor);
    var strategy = new TaskSchedulersGracefulShutdownStrategy();
    strategy.addTaskScheduler(scheduler);

    Field field = TaskSchedulersGracefulShutdownStrategy.class.getDeclaredField("applicationContext");
    field.setAccessible(true);
    field.set(strategy, new StaticApplicationContext());

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().until(executor::isShutdown);
  }
}