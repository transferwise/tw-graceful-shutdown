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

  public ExecutorServiceGracefulShutdownStrategy(ApplicationContext applicationContext, GracefulShutdownProperties gracefulShutdownProperties) {
    super(ExecutorService.class, applicationContext, gracefulShutdownProperties);
  }

  @Override
  protected Mono<Void> shutdownResourceGraceful(@NonNull ExecutorService resource) {
    return Mono.fromRunnable(() -> ExecutorShutdownUtils.shutdownExecutor(resource, true));
  }

  @Override
  protected Mono<Void> shutdownResourceForced(@NonNull ExecutorService resource) {
    return Mono.fromRunnable(resource::shutdownNow);
  }

  @Override
  protected Mono<Boolean> getResourceGracefulTerminationStatus(ExecutorService resource) {
    return Mono.fromCallable(resource::isTerminated);
  }

  @Override
  protected Mono<Boolean> getResourceForcedTerminationStatus(ExecutorService resource) {
    return getResourceGracefulTerminationStatus(resource);
  }
}
