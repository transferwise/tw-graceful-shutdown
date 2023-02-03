package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
public abstract class BaseReactiveResourceShutdownStrategy<T> implements GracefulShutdownStrategy {

  public BaseReactiveResourceShutdownStrategy(@NonNull Class<T> resourceType, @NonNull ApplicationContext applicationContext,
      @NonNull GracefulShutdownProperties gracefulShutdownProperties) {
    this.resourceType = resourceType;
    this.applicationContext = applicationContext;
    this.gracefulShutdownProperties = gracefulShutdownProperties;
  }


  @NonNull
  // Because of Java type erasure we can't get type of generic parameter after compilation.
  // To access it at runtime we have to provide and save it explicitly.
  private final Class<T> resourceType;

  protected Class<T> getResourceType() {
    return this.resourceType;
  }

  @NonNull
  private final ApplicationContext applicationContext;


  @Getter(AccessLevel.PROTECTED)
  @NonNull
  private final GracefulShutdownProperties gracefulShutdownProperties;

  /**
   * Will delay strategy shutdown for this {@link Duration}.
   * @return {@link Duration}
   */
  protected Duration getStrategyShutdownDelay() {
    return Duration.ZERO;
  }

  /**
   * {@link Duration} allowed for strategy to shut down all resources.
   * @return {@link Duration}
   */
  protected Duration getStrategyShutdownTimeout() {
    return Duration.ofMillis(this.gracefulShutdownProperties.getShutdownTimeoutMs());
  }

  /**
   * Time in Ms within resource should shut down.
   * @return Time in Ms
   */
  protected int getResourceFullShutdownTimeoutMs() {
    // resource should shut down a little earlier than StrategyShutdownTimeout
    int result = this.gracefulShutdownProperties.getShutdownTimeoutMs() - 250;
    if (result < 1000) {
      result = this.gracefulShutdownProperties.getShutdownTimeoutMs();
    }

    return result;
  }

  /**
   * {@link Duration} allowed for resource to shut down gracefully. If not shut down within this time -
   * {@link BaseReactiveResourceShutdownStrategy#shutdownResourceForced} will be called.
   * @return {@link Duration}
   */
  protected Duration getResourceGracefulShutdownTimeout() {
    // use 9/10 of time full shut down time to graceful shutdown
    return Duration.ofMillis(getResourceFullShutdownTimeoutMs() / 10 * 9);
  }

  /**
   * {@link Duration} allowed for resource to force shut down. If not shut down within this time - error will be logged.
   * @return {@link Duration}
   */
  protected Duration getResourceForcedShutdownTimeout() {
    // use 1/10 of time full shut down time to force shutdown
    return Duration.ofMillis(getResourceFullShutdownTimeoutMs() / 10);
  }


  @Getter(AccessLevel.PROTECTED)
  private final Scheduler shutdownScheduler = Schedulers.newBoundedElastic(
      10,
      100_000,
      "ShutdownWorker_" + this.getClass().getSimpleName()
  );

  private final List<T> addedResources = new ArrayList<>();

  private final AtomicBoolean isShutdownAllowed = new AtomicBoolean(false);

  /**
   * Default implementation of {@link GracefulShutdownStrategy#prepareForShutdown()}. Will search in {@link ApplicationContext} for Beans of
   * {@link Class} type provided by {@link BaseReactiveResourceShutdownStrategy#getResourceType()} union them wth externally added resources by
   * {@link BaseReactiveResourceShutdownStrategy#addResource} and then shut down them all.
   */
  @Override
  public void prepareForShutdown() {

    Collection<T> resourceBeans = applicationContext.getBeansOfType(getResourceType()).values();
    Set<T> allResources = new HashSet<>(resourceBeans);
    allResources.addAll(addedResources);

    shutdownResources(allResources)
        .delaySubscription(getStrategyShutdownDelay(), getShutdownScheduler())
        .subscribeOn(getShutdownScheduler())
        .subscribe();
  }

  private Mono<Void> shutdownResources(@NonNull Collection<T> shutdownResources) {
    return Flux.fromIterable(shutdownResources)
        // Will start shutting down in parallel
        .parallel()
        .runOn(getShutdownScheduler())
        .flatMap(resource -> {
          String resourceName = getResourceType().getSimpleName();
          String resourceDescription = resource.toString();

          return this.shutdownResourceGraceful(resource)
              .doOnError((throwable) -> log.warn("Error while graceful shutting down {}", resourceName, throwable))
              .doOnSuccess((e) -> log.info("{} gracefully stopped: {}.", resourceName, resourceDescription))
              .doOnSubscribe(e -> log.info("Shutting down {} gracefully: '{}'.", resourceName, resourceDescription))
              .timeout(this.getResourceGracefulShutdownTimeout())
              // Our flow will try to force shut down in case of error on graceful shutting down any resource
              .onErrorResume(throwable ->
                  shutdownResourceForced(resource)
                      .doOnSuccess((e) -> log.info("{} force stopped: {}.", resourceName, resourceDescription))
                      .doOnSubscribe(e -> log.info("Shutting down {} forcefully: '{}'.", resourceName, resourceDescription))
                      .timeout(this.getResourceForcedShutdownTimeout()))
              // Our flow will continue in case of error on force shut down any resource
              .onErrorResume(throwableForce -> {
                log.error("Error while shutting down {}", resourceName, throwableForce);
                return Mono.empty();
              });
        })
        // We interested only when everything is completed, so skip individual signals of shut down resources
        .then()
        // Should fail with error in case all resources is not shut down in time.
        .timeout(this.getStrategyShutdownTimeout())
        .doOnSubscribe((s) -> log.info("Starting shutdown of resources: {}", getResourceType().getSimpleName()))
        .doOnError(throwable -> log.error("Error while shutting down all {}", getResourceType().getSimpleName(), throwable))
        .doOnSuccess((s) -> log.info("All resources stopped"))
        .doOnTerminate(() -> isShutdownAllowed.set(true));
  }

  /**
   * Method will be called to gracefully shut down resource.
   * <p>
   * Example: {@link ExecutorServiceGracefulShutdownStrategy#shutdownResourceGraceful}
   * </p>
   *
   * @param resource Resource to shut down.
   * @return {@link Mono} with graceful shutdown task.
   */
  protected abstract Mono<Void> shutdownResourceGraceful(@NonNull T resource);

  /**
   * Method will be called to force shut down resource if graceful shutdown is failed.
   * <p>
   * Example: {@link ExecutorServiceGracefulShutdownStrategy#shutdownResourceForced}
   * </p>
   *
   * @param resource Resource to shut down.
   * @return {@link Mono} with force shutdown task.
   */
  protected abstract Mono<Void> shutdownResourceForced(@NonNull T resource);


  /**
   * Will shut down gracefully added resources during app shutdown.
   * @param resource Resource to shut down gracefully.
   */
  public void addResource(T resource) {
    addedResources.add(resource);
  }

  @Override
  public boolean canShutdown() {
    return isShutdownAllowed.get();
  }
}
