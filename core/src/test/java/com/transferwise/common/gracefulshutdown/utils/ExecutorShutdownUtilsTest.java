package com.transferwise.common.gracefulshutdown.utils;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Execution(ExecutionMode.CONCURRENT)
class ExecutorShutdownUtilsTest {

  private static final Duration submittedTaskRunTime = Duration.ofSeconds(2);
  private static final Duration checkMaxWaitTime = Duration.ofSeconds(25);

  @Test
  @SneakyThrows
  void waits_till_task_is_completed_for_ThreadPoolTaskScheduler() {
    // GIVEN
    AtomicBoolean isCompleted = new AtomicBoolean(false);
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.initialize();
    ScheduledThreadPoolExecutor internalExecutor = (ScheduledThreadPoolExecutor) threadPoolTaskScheduler.getScheduledExecutor();

    threadPoolTaskScheduler.execute(() -> {
      try {
        Thread.sleep(submittedTaskRunTime.toMillis());
        isCompleted.set(true);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    // wait till task is started before requesting shutdown
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> internalExecutor.getActiveCount() == 1);

    // WHEN
    ExecutorShutdownUtils.shutdownExecutor(threadPoolTaskScheduler, false);

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(isCompleted::get);
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> threadPoolTaskScheduler.getScheduledThreadPoolExecutor().isTerminated());
  }

  @Test
  @SneakyThrows
  void if_internal_scheduler_shutdown_first_ThreadPoolTaskScheduler_waits_till_task_is_completed() {
    // GIVEN
    AtomicBoolean isCompleted = new AtomicBoolean(false);
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.initialize();
    ScheduledThreadPoolExecutor internalExecutor = (ScheduledThreadPoolExecutor) threadPoolTaskScheduler.getScheduledExecutor();

    threadPoolTaskScheduler.execute(() -> {
      try {
        Thread.sleep(submittedTaskRunTime.toMillis());
        isCompleted.set(true);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    // wait till task is started before requesting shutdown
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> internalExecutor.getActiveCount() == 1);

    // WHEN
    ExecutorShutdownUtils.shutdownExecutor(internalExecutor, false);
    ExecutorShutdownUtils.shutdownExecutor(threadPoolTaskScheduler, false);

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(isCompleted::get);
    Awaitility.await().atMost(checkMaxWaitTime).until(internalExecutor::isTerminated);
  }

  @Test
  void waits_till_task_is_completed_for_ConcurrentTaskScheduler() {
    // GIVEN
    AtomicBoolean isCompleted = new AtomicBoolean(false);
    ConcurrentTaskScheduler concurrentTaskScheduler = new ConcurrentTaskScheduler();

    concurrentTaskScheduler.execute(() -> {
      try {
        Thread.sleep(submittedTaskRunTime.toMillis());
        isCompleted.set(true);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    // WHEN
    ExecutorShutdownUtils.shutdownExecutor(concurrentTaskScheduler, false);

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(isCompleted::get);
    Awaitility.await().atMost(checkMaxWaitTime).until(() -> {
      Executor executor = concurrentTaskScheduler.getConcurrentExecutor();
      if (executor instanceof ExecutorService) {
        return ((ExecutorService) executor).isTerminated();
      } else {
        return true;
      }
    });
  }

  @Test
  void waits_till_task_is_completed_for_ScheduledThreadPoolExecutor() {
    // GIVEN
    AtomicBoolean isCompleted = new AtomicBoolean(false);
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    scheduledThreadPoolExecutor.execute(() -> {
      try {
        Thread.sleep(submittedTaskRunTime.toMillis());
        isCompleted.set(true);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    Awaitility.await().atMost(checkMaxWaitTime).until(() -> scheduledThreadPoolExecutor.getActiveCount() == 1);

    // WHEN
    ExecutorShutdownUtils.shutdownExecutor(scheduledThreadPoolExecutor, false);

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(isCompleted::get);
    Awaitility.await().atMost(checkMaxWaitTime).until(scheduledThreadPoolExecutor::isTerminated);
  }

  @Test
  void waits_till_task_is_completed_for_ExecutorService() {
    // GIVEN
    AtomicBoolean isCompleted = new AtomicBoolean(false);
    ExecutorService executorService = Executors.newFixedThreadPool(1);

    executorService.execute(() -> {
      try {
        Thread.sleep(submittedTaskRunTime.toMillis());
        isCompleted.set(true);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    // WHEN
    ExecutorShutdownUtils.shutdownExecutor(executorService, false);

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(isCompleted::get);
    Awaitility.await().atMost(checkMaxWaitTime).until(executorService::isTerminated);
  }
}