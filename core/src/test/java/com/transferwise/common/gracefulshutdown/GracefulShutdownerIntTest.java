package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import com.transferwise.common.gracefulshutdown.strategies.KagkarlssonDbScheduledTaskShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.TaskSchedulersGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.BaseRequestCountGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.test.BaseTestEnvironment;
import com.transferwise.common.gracefulshutdown.test.TestApplication;
import java.time.Duration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@BaseTestEnvironment
// Force a separate ApplicationContext, because we will close this one.
@ActiveProfiles({"test", "shutdown1"})
class GracefulShutdownerIntTest {

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownHealthIndicator healthIndicator;
  @Autowired
  private GracefulShutdownHealthStrategy healthStrategy;
  @Autowired
  private BaseRequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;
  @Autowired
  private KagkarlssonDbScheduledTaskShutdownStrategy kagkarlssonDbScheduledTaskShutdownStrategy;
  @Autowired
  private TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy;
  @Autowired
  private TestApplication testApplication;
  @Autowired
  private ApplicationContext context;

  @Test
  @SneakyThrows
  void testThatItGenerallyWorks() {
    final var schedulerCounterValue = testApplication.schedulerCounter.get();
    assertThat(gracefulShutdowner.isRunning()).isTrue();

    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(healthStrategy);
    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(requestCountGracefulShutdownStrategy);
    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(kagkarlssonDbScheduledTaskShutdownStrategy);
    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(taskSchedulersGracefulShutdownStrategy);

    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);

    await().until(() -> testApplication.schedulerCounter.get() > schedulerCounterValue);

    gracefulShutdowner.stop();

    final var newSchedulerCounterValue = testApplication.schedulerCounter.get();

    await().atMost(Duration.ofSeconds(1)).until(() -> !gracefulShutdowner.isRunning());
    assertThat(testApplication.schedulerCounter.get()).isEqualTo(newSchedulerCounterValue);

    assertThat(gracefulShutdowner.isRunning()).isFalse();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

}
