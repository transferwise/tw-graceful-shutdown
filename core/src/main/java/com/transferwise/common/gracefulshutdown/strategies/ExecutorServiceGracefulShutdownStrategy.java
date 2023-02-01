package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.utils.ExecutorShutdownUtils;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

@Slf4j
public class ExecutorServiceGracefulShutdownStrategy extends BaseReactiveResourceShutdownStrategy<ExecutorService> {

  private final Duration delayBetweenTerminationCheck = Duration.ofMillis(250);

  @Override
  protected Duration getStrategyShutdownDelay() {
    // In case shutdown was called right after call to endpoint:
    // this will give time for endpoint using ExecutorService to send task if required.

    // This is for cases then app is configured incorrectly
    if (getGracefulShutdownProperties().getClientsReactionTimeMs() > getResourceFullShutdownTimeoutMs()) {
      return Duration.ofMillis(getResourceFullShutdownTimeoutMs());
    }

    // we give 1/3 of resource shutdown time to allow endpoints called right before client reaction
    // to proceed and successfully submit tasks
    return Duration.ofMillis(getGracefulShutdownProperties().getClientsReactionTimeMs() + getResourceFullShutdownTimeoutMs() / 3);
  }

  @Override
  protected Duration getResourceGracefulShutdownTimeout() {
    // getStrategyShutdownDelay time of getResourceFullShutdownTimeoutMs was already used.
    // use 9/10 of remaining time for graceful shutdown
    return Duration.ofMillis((getResourceFullShutdownTimeoutMs() - getStrategyShutdownDelay().toMillis()) / 10 * 9);
  }

  @Override
  protected Duration getResourceForcedShutdownTimeout() {
    // use remaining time for forced shutdown
    return Duration.ofMillis((getResourceFullShutdownTimeoutMs() - getStrategyShutdownDelay().toMillis()) / 10);
  }

  public ExecutorServiceGracefulShutdownStrategy(ApplicationContext applicationContext, GracefulShutdownProperties gracefulShutdownProperties) {
    super(ExecutorService.class, applicationContext, gracefulShutdownProperties);
  }

  @Override
  protected Mono<Void> shutdownResourceGraceful(@NonNull ExecutorService resource) {
    return Mono.fromRunnable(() -> ExecutorShutdownUtils.shutdownExecutor(resource, true))
        // Do not emit complete until ExecutorService termination
        .then(waitTermination(resource));
  }

  @Override
  protected Mono<Void> shutdownResourceForced(@NonNull ExecutorService resource) {
    return Mono.fromRunnable(resource::shutdownNow)
        .then(waitTermination(resource));
  }

  private Mono<Boolean> checkTermination(ExecutorService resource) {
    return Mono.fromCallable(resource::isTerminated);
  }

  /**
   * Will check for termination status in non-blocking way. No thread will be waiting.
   *
   * @param resource {@link ExecutorService} to wait for termination.
   * @return {@link Mono} that will complete only when {@link #checkTermination} will return true
   */
  private Mono<Void> waitTermination(ExecutorService resource) {
    return checkTermination(resource)
        // Use expand as this allows to repeatedly call functions based on previous call result with a breadth-first approach.
        // Call stack will not be polluted.
        .expand(isTerminated -> {
          if (!isTerminated) {
            return checkTermination(resource)
                .delaySubscription(this.delayBetweenTerminationCheck);
          } else {
            // Empty Mono signals as exit from expand
            return Mono.empty();
          }
        }).then();
  }
}
