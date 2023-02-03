package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import reactor.core.publisher.Mono;

@Slf4j
@Execution(ExecutionMode.CONCURRENT)
class ExecutorServiceGracefulShutdownStrategyTest {

  private static final Duration submittedTaskRunTime = Duration.ofSeconds(30);
  private static final Duration checkMaxWaitTime = Duration.ofSeconds(25);

  private static final GracefulShutdownProperties gracefulShutdownProperties;

  static {
    gracefulShutdownProperties = new GracefulShutdownProperties();
    gracefulShutdownProperties.setShutdownTimeoutMs((int) Duration.ofSeconds(5).toMillis());
  }

  @Test
  public void shutdown_invoked_on_application_context_classes() {
    // GIVEN
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    ExecutorServiceGracefulShutdownStrategy strategy = new ExecutorServiceGracefulShutdownStrategy(
        applicationContext,
        gracefulShutdownProperties
    );
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("test", scheduledThreadPoolExecutor);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(scheduledThreadPoolExecutor::isTerminated);
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
  }

  @Test
  public void shutdown_invoked_on_external_added_classes() {
    // GIVEN
    ExecutorServiceGracefulShutdownStrategy strategy = new ExecutorServiceGracefulShutdownStrategy(
        new StaticApplicationContext(),
        gracefulShutdownProperties
    );
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    strategy.addResource(executorService);

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(executorService::isTerminated);
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
  }

  @Test
  public void shutdown_timeout_is_applied_and_called_shutdownNow() {
    // GIVEN
    AtomicBoolean isInterrupted = new AtomicBoolean(false);

    ExecutorServiceGracefulShutdownStrategy strategy = new ExecutorServiceGracefulShutdownStrategy(
        new StaticApplicationContext(),
        gracefulShutdownProperties
    );
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    strategy.addResource(executorService);
    executorService.execute(() -> {
      try {
        Thread.sleep(checkMaxWaitTime.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        isInterrupted.set(true);
      }
    });

    // WHEN
    strategy.prepareForShutdown();

    // THEN
    Awaitility.await().atMost(checkMaxWaitTime).until(executorService::isTerminated);
    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
    Assertions.assertTrue(isInterrupted.get());
  }

  @Test
  @Disabled("For development only")
  public void shutdown_runs_in_multiple_treads() {
    // GIVEN
    int processorsCount = Runtime.getRuntime().availableProcessors();
    if (processorsCount < 2) {
      log.warn("Test requires proper parallel execution");
      return;
    }

    ExecutorServiceGracefulShutdownStrategy strategy = new ExecutorServiceGracefulShutdownStrategy(
        new StaticApplicationContext(),
        gracefulShutdownProperties
    ) {
      // need to add blocking as ExecutorService.shutdown() returns immediately
      @Override
      protected Mono<Void> shutdownResourceGraceful(@NonNull ExecutorService resource) {
        try {
          // Waiting here, so we block calling thread.
          // Goal is to fail test if not run in multiple threads.
          // Because each thread will sleep for submittedTaskRunTime seconds before shutdown,
          // it will be unable to start shutting down all executors before shutdown check completes
          // ExecutorService count * sleep time > Wait time for ExecutorService::isShutdown check

          //noinspection BlockingMethodInNonBlockingContext
          Thread.sleep(gracefulShutdownProperties.getShutdownTimeoutMs() / 3);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        log.info("Waiting complete");

        return super.shutdownResourceGraceful(resource)
            .doOnSubscribe((s) -> log.info("Starting shutdown"));
      }
    };

    List<ExecutorService> executorServiceList = new ArrayList<>();

    for (int executorServiceCount = 0; executorServiceCount < 8; executorServiceCount++) {
      ExecutorService executorService = Executors.newFixedThreadPool(1);
      executorServiceList.add(executorService);
      executorService.execute(() -> {
        try {
          Thread.sleep(submittedTaskRunTime.toMillis());
          log.info("Completed");
        } catch (InterruptedException e) {
          log.info("Task interrupted");
        }
      });

      strategy.addResource(executorService);
    }

    // WHEN
    strategy.prepareForShutdown();

    // THEN

    // shutdown should be called almost immediately if shutdown started in parallel
    Awaitility.await().atMost(Duration.ofMillis(gracefulShutdownProperties.getShutdownTimeoutMs())).until(() ->
        executorServiceList.stream().allMatch(ExecutorService::isShutdown)
    );

    // all task should complete within this time if run in parallel
    Awaitility.await().atMost(checkMaxWaitTime).until(() ->
        executorServiceList.stream().allMatch(ExecutorService::isTerminated)
    );

    Awaitility.await().atMost(checkMaxWaitTime).until(strategy::canShutdown);
  }
}