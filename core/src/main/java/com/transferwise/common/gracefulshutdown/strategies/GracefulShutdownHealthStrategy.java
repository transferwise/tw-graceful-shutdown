package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GracefulShutdownHealthStrategy implements GracefulShutdownStrategy {

  @Getter
  private volatile boolean shutdownInProgress;

  @Getter
  private volatile boolean startupInProgress = true;

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

  @Override
  public boolean isReady() {
    return !startupInProgress;
  }
}
