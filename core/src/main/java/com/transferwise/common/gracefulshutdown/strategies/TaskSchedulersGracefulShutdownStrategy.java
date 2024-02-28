package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.utils.ExecutorShutdownUtils;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import reactor.core.publisher.Mono;

@Slf4j
public class TaskSchedulersGracefulShutdownStrategy extends BaseReactiveResourceShutdownStrategy<TaskScheduler> {

  private static final Class taskSchedulerRouter;
  private static final Field taskSchedulerRouterLocalExecutorField;
  private static final Method taskSchedulerRouterDestoryMethod;

  static {
    Class clazz = null;
    Field field = null;
    Method method = null;
    try {
      clazz = Class.forName("org.springframework.scheduling.config.TaskSchedulerRouter", true, ExecutorShutdownUtils.class.getClassLoader());
      field = clazz.getDeclaredField("localExecutor");
      field.setAccessible(true);
      method = clazz.getDeclaredMethod("destroy");
    } catch (ClassNotFoundException e) {
      // ignore as it's normal in pre Spring 6.1 environment.
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      throw new RuntimeException("org.springframework.scheduling.config.TaskSchedulerRouter class is missing expected field or method. Contact the SRE team.", e);
    }
    taskSchedulerRouter = clazz;
    taskSchedulerRouterLocalExecutorField = field;
    taskSchedulerRouterDestoryMethod = method;
  }

  @Setter
  private ScheduledTaskRegistrar taskRegistrar;

  @Override
  protected Duration getStrategyShutdownDelay() {
    // Handling of @Scheduled annotation should stop immediately.
    // Those are not related to clients' requests.

    return Duration.ofSeconds(0);
  }

  public TaskSchedulersGracefulShutdownStrategy(ApplicationContext applicationContext, GracefulShutdownProperties gracefulShutdownProperties) {
    super(TaskScheduler.class, applicationContext, gracefulShutdownProperties);
  }

  @Override
  protected Mono<Void> shutdownResourceGraceful(@NonNull TaskScheduler resource) {
    return Mono.fromRunnable(() -> {
      if (resource instanceof Executor) {
        ExecutorShutdownUtils.shutdownExecutor((Executor) resource, false);
      } else if (taskSchedulerRouter != null && taskSchedulerRouter.isInstance(resource)) {
        shutdownTaskSchedulerRouter(resource);
      } else {
        log.info("Shutting down unknown task scheduler '{}' using it's 'shutdown()' method.", resource);
        ExecutorShutdownUtils.shutdownExecutorWithReflection(resource, true);
      }
    });
  }

  @Override
  protected Mono<Void> shutdownResourceForced(@NonNull TaskScheduler resource) {
    return Mono.fromRunnable(() -> {
      if (resource instanceof Executor) {
        ExecutorShutdownUtils.shutdownExecutorForced((Executor) resource);
      } else if (taskSchedulerRouter != null && taskSchedulerRouter.isInstance(resource)) {
        shutdownTaskSchedulerRouterForced(resource);
      } else {
        log.warn("Unknown TaskScheduler to force shutdown: {}. Skipping.", resource.getClass());
      }
    });
  }

  @Override
  protected Mono<Boolean> getResourceGracefulTerminationStatus(TaskScheduler resource) {
    return Mono.fromCallable(() -> {
      if (resource instanceof Executor) {
        return ExecutorShutdownUtils.isTerminated((Executor) resource);
      } else {
        log.warn("Unknown TaskScheduler to check termination: {}. Return true.", resource.getClass());
        return true;
      }
    });
  }

  @Override
  protected Mono<Boolean> getResourceForcedTerminationStatus(TaskScheduler resource) {
    return getResourceGracefulTerminationStatus(resource);
  }

  @Override
  public Set<TaskScheduler> getResourcesForShutdown() {
    Set<TaskScheduler> resourcesForShutdown = super.getResourcesForShutdown();
    if (taskRegistrar != null) {
      resourcesForShutdown.add(taskRegistrar.getScheduler());
    }
    return resourcesForShutdown;
  }

  private static void shutdownTaskSchedulerRouter(TaskScheduler scheduler) {
    try {
      Executor executor = (Executor) taskSchedulerRouterLocalExecutorField.get(scheduler);
      if (executor != null) {
        ExecutorShutdownUtils.shutdownExecutor(executor, true);
      }
    } catch (IllegalAccessException e) {
      log.warn("Couldn't shutdown TaskSchedulerRouter during graceful shutdown", e);
    }
  }

  private void shutdownTaskSchedulerRouterForced(TaskScheduler scheduler) {
    try {
      taskSchedulerRouterDestoryMethod.invoke(scheduler);
    } catch (IllegalAccessException | InvocationTargetException e) {
      log.warn("Couldn't force shutdown TaskSchedulerRouter during graceful shutdown", e);
    }
  }

}
