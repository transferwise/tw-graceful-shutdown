package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import com.transferwise.common.gracefulshutdown.strategies.KagkarlssonDbScheduledTaskShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.RequestCountGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.TaskSchedulersGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.test.TestApplication;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test"})
@SpringBootTest(classes = {TestApplication.class})
class GracefulShutdownerIntTest {

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownHealthIndicator healthIndicator;
  @Autowired
  private GracefulShutdownHealthStrategy healthStrategy;
  @Autowired
  private RequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;
  @Autowired
  private KagkarlssonDbScheduledTaskShutdownStrategy kagkarlssonDbScheduledTaskShutdownStrategy;
  @Autowired
  private TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy;
  @Autowired
  private TestApplication testApplication;

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

    // No idea, how to make it without sleep. We want to check if the scheduling stopped.
    Thread.sleep(50);
    assertThat(testApplication.schedulerCounter.get()).isEqualTo(newSchedulerCounterValue);

    assertThat(gracefulShutdowner.isRunning()).isFalse();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

}
