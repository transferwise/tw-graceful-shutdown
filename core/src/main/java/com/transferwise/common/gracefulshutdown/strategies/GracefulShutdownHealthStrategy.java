package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class GracefulShutdownHealthStrategy implements GracefulShutdownStrategy {

  @Getter
  private volatile boolean shutdownInProgress;

  @Getter
  private volatile Long startupTime = null;

  private AtomicBoolean readyDeclared = new AtomicBoolean(false);

  @Autowired
  private GracefulShutdownProperties gracefulShutdownProperties;

  @Override
  public void applicationStarted() {
    startupTime = System.currentTimeMillis();
    shutdownInProgress = false;

    if (gracefulShutdownProperties.getStartupHealthyDelayMs() > 0) {
      log.info("Creating artificial delay of {} ms before allowing to report healthy status.", gracefulShutdownProperties.getStartupHealthyDelayMs());
    }
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
    var ready = startupTime != null && System.currentTimeMillis() - startupTime > gracefulShutdownProperties.getStartupHealthyDelayMs();

    if (!readyDeclared.getAndSet(true)) {
      log.info("Considering Service as fully started. Starting to broadcast UP state.");
    }

    return ready;
  }
}
