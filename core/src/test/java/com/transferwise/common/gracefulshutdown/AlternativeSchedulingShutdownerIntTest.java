package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.transferwise.common.gracefulshutdown.strategies.TaskSchedulersGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.test.TestApplication;
import com.transferwise.common.gracefulshutdown.test.TestBApplication;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test"})
@SpringBootTest(classes = {TestBApplication.class})
class AlternativeSchedulingShutdownerIntTest {

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;
  @Autowired
  private TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy;
  @Autowired
  private TestApplication testApplication;

  @Test
  @SneakyThrows
  void testThatItGenerallyWorks() {
    final var schedulerCounterValue = testApplication.schedulerCounter.get();
    assertThat(gracefulShutdowner.isRunning()).isTrue();

    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(taskSchedulersGracefulShutdownStrategy);

    await().until(() -> testApplication.schedulerCounter.get() > schedulerCounterValue);

    gracefulShutdowner.stop();

    final var newSchedulerCounterValue = testApplication.schedulerCounter.get();

    // No idea, how to make it without sleep. We want to check if the scheduling stopped.
    Thread.sleep(50);
    assertThat(testApplication.schedulerCounter.get()).isEqualTo(newSchedulerCounterValue);

    assertThat(gracefulShutdowner.isRunning()).isFalse();
  }

}
