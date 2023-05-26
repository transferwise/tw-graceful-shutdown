package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownIgnore;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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

  @Getter(AccessLevel.PROTECTED)
  private final Scheduler shutdownScheduler = Schedulers.newBoundedElastic(
      10,
      100_000,
      "ShutdownWorker_" + this.getClass().getSimpleName()
  );

  private final List<T> addedResources = new ArrayList<>();

  private final AtomicBoolean isShutdownAllowed = new AtomicBoolean(false);


  /**
   * {@link Duration} allowed for resource to force shut down. If not shut down within this time - error will be logged.
   *
   * @return {@link Duration}
   */
  private Duration getResourceForcedShutdownTimeout() {
    // This checks incorrect configuration. Timeout error will be logged.
    if (getStrategyShutdownDelay().toMillis() > getResourceFullShutdownTimeoutMs()) {
      return Duration.ZERO;
    }

    // use remaining time for forced shutdown
    return Duration.ofMillis(getResourceFullShutdownTimeoutMs() - getResourceGracefulShutdownTimeout().toMillis());
  }

  /**
   * Interval between checks of the resource termination status. Used by {@link #waitTermination}
   *
   * @return {@link Duration} between termination check
   */
  private Duration getDelayBetweenTerminationCheck() {
    return Duration.ofMillis(this.getGracefulShutdownProperties().getResourceCheckIntervalTimeMs());
  }

  /**
   * Will delay strategy shutdown for this {@link Duration}.
   * This will additionally affect {@link #getResourceGracefulShutdownTimeout}
   * and {@link #getResourceForcedShutdownTimeout}
   * <p>
   *   Read {@link #getResourceGracefulShutdownTimeShare} for more info.
   * </p>
   *
   * @return {@link Duration} to delay strategy shutdown.
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
   * Split ResourceFullShutdownTimeout between graceful and forced shutdown. Value between 0 and 1 is expected where 1 means 100%.
   * <ul>
   *   <li>{@link #getResourceGracefulShutdownTimeout} =
   *   ({@link #getResourceFullShutdownTimeoutMs} -
   *   {@link #getStrategyShutdownDelay}) *
   *   {@link #getResourceGracefulShutdownTimeShare}
   *   </li>
   *   <li>{@link #getResourceForcedShutdownTimeout} =
   *   {@link #getResourceFullShutdownTimeoutMs} -
   *   {@link #getResourceGracefulShutdownTimeout}
   *   </li>
   * </ul>
   *
   * @return time share of ResourceFullShutdownTimeout dedicated to graceful shutdown
   */
  protected double getResourceGracefulShutdownTimeShare() {
    return 0.9d;
  }

  /**
   * {@link Duration} allowed for resource to shut down gracefully. If not shut down within this time -
   * {@link #shutdownResourceForced} will be called.
   *
   * @return {@link Duration}
   */
  private Duration getResourceGracefulShutdownTimeout() {
    // This checks incorrect configuration. Timeout error will be logged.
    if (getStrategyShutdownDelay().toMillis() > getResourceFullShutdownTimeoutMs()) {
      return Duration.ZERO;
    }

    // getStrategyShutdownDelay time of getResourceFullShutdownTimeoutMs was already used.
    // use ResourceGracefulShutdownTimeShare of remaining time for graceful shutdown
    double gracefulShutdownShare = Math.min(getResourceGracefulShutdownTimeShare(), 1d);

    int resourceGracefulShutdownTimeMS = (int) ((getResourceFullShutdownTimeoutMs() - getStrategyShutdownDelay().toMillis()) * gracefulShutdownShare);
    return Duration.ofMillis(resourceGracefulShutdownTimeMS);
  }

  /**
   * Prepare {@link Set} of resources for shutdown by extracting Beans of {@link #getResourceType()} from application context
   * and add manually added resources by {@link #addResource}.
   * @return {@link Set} of resources for shutdown
   */
  public Set<T> getResourcesForShutdown() {
    Set<Object> ignoredBeans = applicationContext.getBeansWithAnnotation(GracefulShutdownIgnore.class)
        .values().stream()
        .collect(Collectors.toUnmodifiableSet());

    Set<T> allResources = applicationContext.getBeansOfType(getResourceType())
        .values().stream()
        .filter(e -> !ignoredBeans.contains(e))
        .collect(Collectors.toSet());

    allResources.addAll(addedResources);

    return allResources;
  }

  /**
   * Default implementation of {@link GracefulShutdownStrategy#prepareForShutdown()}. Will search in {@link ApplicationContext} for Beans of
   * {@link Class} type provided by {@link #getResourceType()} union them wth externally added resources by
   * {@link #addResource} and then shut down them all.
   */
  @Override
  public void prepareForShutdown() {

    Set<T> allResources = getResourcesForShutdown();

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
              // Do not emit complete until resource termination
              .then(waitTermination(getResourceGracefulTerminationStatus(resource)))
              .doOnError((throwable) -> log.warn("Error while graceful shutting down {}", resourceName, throwable))
              .doOnSuccess((e) -> log.info("{} gracefully stopped: {}.", resourceName, resourceDescription))
              .doOnSubscribe(e -> log.info("Shutting down {} gracefully: '{}'.", resourceName, resourceDescription))
              .timeout(this.getResourceGracefulShutdownTimeout())
              // Our flow will try to force shut down in case of error on graceful shutting down any resource
              .onErrorResume(throwable ->
                  shutdownResourceForced(resource)
                      // Do not emit complete until resource termination
                      .then(waitTermination(getResourceForcedTerminationStatus(resource)))
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
   * This method will be called by {@link #waitTermination} for checking graceful shutdown status.
   * @param resource Resource to shut down.
   * @return {@link Mono} with graceful termination check task. Should return true for terminated resource.
   */
  protected abstract Mono<Boolean> getResourceGracefulTerminationStatus(T resource);

  /**
   * This method will be called by {@link #waitTermination} for checking forced shutdown status.
   * @param resource Resource to shut down.
   * @return {@link Mono} with forced termination check task. Should return true for terminated resource.
   */
  protected abstract Mono<Boolean> getResourceForcedTerminationStatus(T resource);

  /**
   * Will check for termination status in non-blocking way. No thread will be waiting.
   *
   * @param checkTerminationMono {@link Mono} to wait for termination.
   * @return {@link Mono} that will complete only when {@link #getResourceGracefulTerminationStatus} for graceful shutdown check
   *         or {@link #getResourceForcedTerminationStatus} for forced shutdown check will return true.
   */
  private Mono<Void> waitTermination(Mono<Boolean> checkTerminationMono) {
    return checkTerminationMono
        // Use expand as this allows to repeatedly call functions based on previous call result with a breadth-first approach.
        // Call stack will not be polluted.
        .expand(isTerminated -> {
          if (!isTerminated) {
            return checkTerminationMono
                .delaySubscription(this.getDelayBetweenTerminationCheck());
          } else {
            // Empty Mono signals as exit from expand
            return Mono.empty();
          }
        })
        // we should submit complete only when resource is terminated
        .then();
  }

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
