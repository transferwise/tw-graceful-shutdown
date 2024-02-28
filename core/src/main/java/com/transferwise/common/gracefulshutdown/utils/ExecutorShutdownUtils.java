package com.transferwise.common.gracefulshutdown.utils;

import com.transferwise.common.baseutils.concurrency.ScheduledTaskExecutor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
public abstract class ExecutorShutdownUtils {

  /**
   * Will execute appropriate methods to shut down Executor.
   *
   * @param executor                     {@link Executor} to shutdown
   * @param askToReportOnUnknownExecutor in case of unknown {@link Executor} and absence of shutdown() method warning will be logged with request to
   *                                     contact SRE
   */
  public static void shutdownExecutor(Executor executor, boolean askToReportOnUnknownExecutor) {

    if (executor instanceof ThreadPoolTaskScheduler) {
      log.info("Shutting down thread pool task scheduler '{}'.", executor);
      shutdownThreadPoolTaskScheduler((ThreadPoolTaskScheduler) executor);
    } else if (executor instanceof ConcurrentTaskScheduler) {
      log.info("Shutting down concurrent task scheduler '{}'.", executor);
      shutdownConcurrentTaskScheduler((ConcurrentTaskScheduler) executor);
    } else if (executor instanceof ScheduledThreadPoolExecutor) {
      log.info("Shutting down scheduled task scheduler '{}'.", executor);
      shutdownScheduledThreadPoolExecutor((ScheduledThreadPoolExecutor) executor);
    } else if (executor instanceof ExecutorService) {
      log.info("Shutting down ExecutorService '{}'.", executor);
      shutdownExecutorService((ExecutorService) executor);
    } else {
      log.info("Shutting down unknown executor '{}' using it's 'shutdown()' method.", executor);
      shutdownExecutorWithReflection(executor, askToReportOnUnknownExecutor);
    }
  }

  /**
   * Will execute appropriate methods to force shut down Executor.
   *
   * @param executor {@link Executor} to shutdown
   */
  public static void shutdownExecutorForced(Executor executor) {
    if (executor instanceof ThreadPoolTaskScheduler) {
      shutdownThreadPoolTaskSchedulerForced((ThreadPoolTaskScheduler) executor);
    } else if (executor instanceof ConcurrentTaskScheduler) {
      ConcurrentTaskScheduler concurrentTaskScheduler = (ConcurrentTaskScheduler) executor;
      shutdownExecutorForced(concurrentTaskScheduler.getConcurrentExecutor());
    } else if (executor instanceof ScheduledThreadPoolExecutor) {
      ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) executor;
      scheduledThreadPoolExecutor.shutdownNow();
    } else if (executor instanceof ExecutorService) {
      ((ExecutorService) executor).shutdownNow();
    } else {
      log.warn("Unknown executor to shutdown: {}", executor.getClass());
    }
  }

  /**
   * Returns true if all tasks have completed following shut down. Note that isTerminated is never true unless either shutdown or shutdownNow was
   * called first.
   *
   * @param executor {@link Executor} to check
   * @return true if all tasks have completed following shut down
   */
  public static boolean isTerminated(Executor executor) {
    if (executor instanceof ThreadPoolTaskScheduler) {
      ThreadPoolTaskScheduler threadPoolTaskScheduler = (ThreadPoolTaskScheduler) executor;
      return threadPoolTaskScheduler.getScheduledThreadPoolExecutor().isTerminated();
    } else if (executor instanceof ConcurrentTaskScheduler) {
      ConcurrentTaskScheduler concurrentTaskScheduler = (ConcurrentTaskScheduler) executor;
      return isTerminated(concurrentTaskScheduler.getConcurrentExecutor());
    } else if (executor instanceof ScheduledThreadPoolExecutor) {
      ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) executor;
      return scheduledThreadPoolExecutor.isTerminated();
    } else if (executor instanceof ExecutorService) {
      return ((ExecutorService) executor).isTerminated();
    } else {
      log.warn("Unknown executor to check for isTerminated: {}", executor.getClass());
      return true;
    }
  }

  /**
   * Will try to get <b>shutdown()</b> method with reflection and run it on provided Object.
   *
   * @param executor                     Executor to shut down
   * @param askToReportOnUnknownExecutor In case of absence of shutdown() method warning will be logged with request to contact SRE
   */
  public static void shutdownExecutorWithReflection(Object executor, boolean askToReportOnUnknownExecutor) {
    try {
      Method shutdownMethod = executor.getClass().getMethod("shutdown");
      shutdownMethod.invoke(executor);
    } catch (NoSuchMethodException noSuchMethodException) {
      if (askToReportOnUnknownExecutor) {
        log.warn("Found an executor '{}', but do not know how to shut it down. Please contact SRE team.",
            executor.getClass().getSimpleName(),
            noSuchMethodException
        );
      }
    } catch (Throwable t) {
      log.error("Shutting down executor failed.", t);
    }
  }

  private static void shutdownExecutorService(ExecutorService executorService) {
    executorService.shutdown();
  }

  private static void shutdownThreadPoolTaskScheduler(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = threadPoolTaskScheduler.getScheduledThreadPoolExecutor();
    scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
    threadPoolTaskScheduler.shutdown();
  }

  private static void shutdownThreadPoolTaskSchedulerForced(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = threadPoolTaskScheduler.getScheduledThreadPoolExecutor();
    // may be stale value before we call clear, but it is still worth to have this visibility
    int tasksInQueueCount = scheduledThreadPoolExecutor.getQueue().size();
    if (tasksInQueueCount > 0) {
      log.warn("Before shutdown {} task was in queue to process", tasksInQueueCount);
    }
    scheduledThreadPoolExecutor.getQueue().clear();
    shutdownExecutorForced(scheduledThreadPoolExecutor);
  }

  private static void shutdownConcurrentTaskScheduler(ConcurrentTaskScheduler concurrentTaskScheduler) {
    Executor executor = concurrentTaskScheduler.getConcurrentExecutor();
    shutdownExecutor(executor, false);
  }

  private static void shutdownScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
    scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    scheduledThreadPoolExecutor.getQueue().clear();
    scheduledThreadPoolExecutor.shutdown();
  }

}
