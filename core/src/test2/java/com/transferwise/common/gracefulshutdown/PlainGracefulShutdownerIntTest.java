package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import com.transferwise.common.gracefulshutdown.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test"})
@SpringBootTest(classes = {TestApplication.class},
    properties = {"tw-graceful-shutdown.clientsReactionTimeMs=1", "tw-graceful-shutdown.shutdownTimeoutMs=1"})
class PlainGracefulShutdownerIntTest {

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private GracefulShutdownHealthIndicator healthIndicator;
  @Autowired
  private GracefulShutdownHealthStrategy healthStrategy;
  @Autowired
  private GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry;

  @Test
  void testThatItGenerallyWorks() {
    assertThat(gracefulShutdowner.isRunning()).isTrue();

    assertThat(gracefulShutdownStrategiesRegistry.getStrategies().size()).isEqualTo(1);
    assertThat(gracefulShutdownStrategiesRegistry.getStrategies()).contains(healthStrategy);

    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);

    gracefulShutdowner.stop();

    assertThat(gracefulShutdowner.isRunning()).isFalse();
    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

}
