package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.utils.ExecutorShutdownUtils;
import java.time.Duration;
import java.util.concurrent.Executor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Mono;

@Slf4j
public class TaskSchedulersGracefulShutdownStrategy extends BaseReactiveResourceShutdownStrategy<TaskScheduler> {

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

  /**
   * Will shut down gracefully added resources during app shutdown.
   * @param taskScheduler TaskScheduler to shut down gracefully.
   */
  public void addTaskScheduler(TaskScheduler taskScheduler) {
    addResource(taskScheduler);
  }
}
