package com.transferwise.common.gracefulshutdown.strategies;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

class TaskSchedulersGracefulShutdownStrategyTest {

    @Test
    public void shutdown_invoked_on_private_classes() throws NoSuchFieldException, IllegalAccessException, ExecutionException, InterruptedException {
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