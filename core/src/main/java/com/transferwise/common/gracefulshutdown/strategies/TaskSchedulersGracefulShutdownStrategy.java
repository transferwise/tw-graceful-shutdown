package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.utils.ExecutorShutdownUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

@Slf4j
@RequiredArgsConstructor
public class TaskSchedulersGracefulShutdownStrategy implements GracefulShutdownStrategy {

  private final ApplicationContext applicationContext;

  private final List<TaskScheduler> taskSchedulers = new ArrayList<>();

  private final AtomicInteger inProgressShutdowns = new AtomicInteger();

  @Override
  public void prepareForShutdown() {
    var executors = Executors.newFixedThreadPool(10);

    var taskSchedulerBeans = applicationContext.getBeansOfType(TaskScheduler.class).values();
    var allTaskSchedulers = new HashSet<>(taskSchedulerBeans);
    allTaskSchedulers.addAll(taskSchedulers);

    for (var taskSchedulerProto : allTaskSchedulers) {
      inProgressShutdowns.incrementAndGet();
      executors.submit(() -> {
        var taskScheduler = taskSchedulerProto;
        try {
          if (taskScheduler instanceof Executor) {
            ExecutorShutdownUtils.shutdownExecutor((Executor) taskScheduler, false);
          } else {
            log.info("Shutting down unknown task scheduler '{}' using it's 'shutdown()' method.", taskScheduler);
            ExecutorShutdownUtils.shutdownExecutorWithReflection(taskScheduler, true);
          }
        } catch (Throwable t) {
          log.error("Stopping a task scheduler failed.", t);
        } finally {
          inProgressShutdowns.decrementAndGet();
        }
      });
    }
  }

  @Override
  public boolean canShutdown() {
    return inProgressShutdowns.get() == 0;
  }

  public void addTaskScheduler(TaskScheduler scheduler) {
    taskSchedulers.add(scheduler);
  }
}
