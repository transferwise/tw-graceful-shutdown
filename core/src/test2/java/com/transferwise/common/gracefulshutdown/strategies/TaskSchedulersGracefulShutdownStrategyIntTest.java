package com.transferwise.common.gracefulshutdown.strategies;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.gracefulshutdown.GracefulShutdownHealthIndicator;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategiesRegistry;
import com.transferwise.common.gracefulshutdown.GracefulShutdowner;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test-fast-shutdown"})
@SpringBootTest(classes = {TaskSchedulersGracefulShutdownStrategyIntTest.TestApplication.class})
class TaskSchedulersGracefulShutdownStrategyIntTest {

  @SpringBootApplication
  @EnableScheduling
  public static class TestApplication {

    @Configuration
    public static class ExecutorsConfig {

      @Bean
      ConcurrentTaskScheduler getConcurrentTaskScheduler() {
        return new ConcurrentTaskScheduler();
      }
    }
  }

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownHealthIndicator healthIndicator;
  @Autowired
  TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;

  @Autowired
  private ConcurrentTaskScheduler concurrentTaskScheduler;

  @Test
  @Disabled("Disabled: Test will fail. Created to show problem with current implementation.")
  void test_when_task_is_running_longer_than_shutdown_timeout() {
    // GIVEN
    assertThat(gracefulShutdowner.isRunning()).isTrue();
    AtomicBoolean isTaskCompletes = new AtomicBoolean(false);
    AtomicBoolean isTaskInterrupted = new AtomicBoolean(false);

    assertThat(gracefulShutdownStrategiesRegistry.getStrategies().contains(taskSchedulersGracefulShutdownStrategy)).isTrue();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
    concurrentTaskScheduler.execute(() -> {
      try {
        Thread.sleep(Duration.ofSeconds(20).toMillis());
        isTaskCompletes.set(true);
      } catch (InterruptedException e) {
        isTaskInterrupted.set(true);
      }
    });

    // WHEN
    gracefulShutdowner.stop();

    // THEN

    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
    assertThat(gracefulShutdowner.isRunning()).isFalse();
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> isTaskCompletes.get() || isTaskInterrupted.get());
  }
}