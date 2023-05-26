package com.transferwise.common.gracefulshutdown.strategies;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.gracefulshutdown.GracefulShutdownHealthIndicator;
import com.transferwise.common.gracefulshutdown.GracefulShutdownIgnore;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategiesRegistry;
import com.transferwise.common.gracefulshutdown.GracefulShutdowner;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test-fast-shutdown"})
@SpringBootTest(classes = {ExecutorServiceGracefulShutdownStrategyIntTest.TestApplication.class})
class ExecutorServiceGracefulShutdownStrategyIntTest {

  @SpringBootApplication
  @EnableScheduling
  public static class TestApplication {

    @Configuration
    public static class ExecutorsConfig {

      @Primary
      @Bean
      ExecutorService getTestExecutorService() {
        return Executors.newFixedThreadPool(1);
      }

      @Bean
      @GracefulShutdownIgnore
      ExecutorService getIgnoredTestExecutorService() {
        return Executors.newFixedThreadPool(1);
      }
    }
  }

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownHealthIndicator healthIndicator;
  @Autowired
  ExecutorServiceGracefulShutdownStrategy executorServiceGracefulShutdownStrategy;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;

  @Autowired
  private ExecutorService testExecutorService;

  @Test
  void test_when_task_is_running_longer_than_shutdown_timeout_then_interrupt() {
    // GIVEN
    assertThat(gracefulShutdowner.isRunning()).isTrue();
    AtomicBoolean isTaskCompleted = new AtomicBoolean(false);
    AtomicBoolean isTaskInterrupted = new AtomicBoolean(false);

    assertThat(gracefulShutdownStrategiesRegistry.getStrategies().contains(executorServiceGracefulShutdownStrategy)).isTrue();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
    testExecutorService.execute(() -> {
      try {
        Thread.sleep(Duration.ofSeconds(20).toMillis());
        isTaskCompleted.set(true);
      } catch (InterruptedException e) {
        isTaskInterrupted.set(true);
      }
    });

    // WHEN
    gracefulShutdowner.stop();

    // THEN

    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
    assertThat(gracefulShutdowner.isRunning()).isFalse();
    Awaitility.await().atMost(Duration.ofSeconds(2)).until(testExecutorService::isTerminated);
    assertThat(isTaskCompleted.get()).isEqualTo(false);
    assertThat(isTaskInterrupted.get()).isEqualTo(true);
  }

  @Test
  void test_when_bean_annotated_with_GracefulShutdownIgnore_then_it_is_ignored_by_graceful_shutdown() {
    // GIVEN

    // WHEN
    Set<ExecutorService> resourcesToShutdown = executorServiceGracefulShutdownStrategy.getResourcesForShutdown();

    // THEN
    Assertions.assertEquals(1, resourcesToShutdown.size());

  }
}