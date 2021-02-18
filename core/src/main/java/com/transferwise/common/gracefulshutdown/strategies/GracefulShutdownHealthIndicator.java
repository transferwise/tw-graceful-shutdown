package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

@Slf4j
public class GracefulShutdownHealthIndicator extends AbstractHealthIndicator implements GracefulShutdownStrategy {

  @Getter
  private boolean shutdownInProgress;

  @Getter
  private boolean startupInProgress = true;

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    if (startupInProgress) {
      builder.down();
    } else if (shutdownInProgress) {
      builder.down();
    } else {
      builder.up();
    }
  }

  @Override
  public void applicationStarted() {
    log.info("Considering Service as fully started. Starting to broadcast UP state.");
    startupInProgress = false;
    shutdownInProgress = false;
  }

  @Override
  public void prepareForShutdown() {
    log.info("Switching to shutdown mode. Starting to broadcast OUT_OF_SERVICE state.");
    shutdownInProgress = true;
  }

  @Override
  public boolean canShutdown() {
    return true;
  }
}
