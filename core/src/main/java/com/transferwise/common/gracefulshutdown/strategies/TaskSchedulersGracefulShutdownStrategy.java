package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
public class TaskSchedulersGracefulShutdownStrategy implements GracefulShutdownStrategy {

  @Autowired
  private ApplicationContext applicationContext;

  private volatile boolean canShutDown = true;

  @Override
  public void prepareForShutdown() {
    var taskSchedulers = applicationContext.getBeansOfType(TaskScheduler.class).values();

    canShutDown = false;
    var executors = Executors.newFixedThreadPool(10);
    executors.submit(() -> {
      try {
        for (var taskScheduler : taskSchedulers) {
          if (taskScheduler instanceof ThreadPoolTaskScheduler) {
            log.info("Shutting down thread pool task scheduler '{}'.", taskScheduler);
            var threadPoolTaskScheduler = (ThreadPoolTaskScheduler) taskScheduler;
            threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
            threadPoolTaskScheduler.shutdown();
          } else if (taskScheduler instanceof ConcurrentTaskScheduler) {
            log.info("Shutting down thread pool task scheduler '{}'.", taskScheduler);
            var concurrentTaskScheduler = (ConcurrentTaskScheduler) taskScheduler;
            var executor = concurrentTaskScheduler.getConcurrentExecutor();

            try {
              var shutdownMethod = executor.getClass().getMethod("shutdown");
              shutdownMethod.invoke(executor);
            } catch (NoSuchMethodException ignored) {
              // ignored
            } catch (Throwable t) {
              log.error("Shutting down concurrent task scheduler executor failed.", t);
            }
          } else {
            boolean shutdownCalled = false;
            try {
              var shutdownMethod = taskScheduler.getClass().getMethod("shutdown");
              log.info("Shutting down unknown task scheduler '{}' using it's 'shutdown()' method.", taskScheduler);
              shutdownMethod.invoke(taskScheduler);
              shutdownCalled = true;
            } catch (NoSuchMethodException ignored) {
              // ignored
            } catch (Throwable t) {
              log.error("Shutting down concurrent task scheduler executor failed.", t);
            }
            if (!shutdownCalled) {
              log.warn("Found a task scheduler '{}', but do not know how to shut it down. Please contact SRE team.", taskScheduler);
            }
          }
        }
      } catch (Throwable t) {
        log.error("Stopping a task scheduler failed.", t);
      } finally {
        canShutDown = true;
      }
    });
  }

  @Override
  public boolean canShutdown() {
    return canShutDown;
  }
}
