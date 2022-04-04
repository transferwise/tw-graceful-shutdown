package com.transferwise.common.gracefulshutdown.strategies;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class KagkarlssonDbScheduledTaskShutdownStrategy implements GracefulShutdownStrategy {

  @Autowired
  private Scheduler scheduler;

  private volatile boolean canShutDown = true;

  @Override
  public void prepareForShutdown() {
    log.info("Attempting to shut down Kagkarlsson scheduler");

    canShutDown = false;

    new Thread(() -> {
      try {
        // `scheduler.stop` is a blocking operation, that is why we have to run it another thread.
        // Default shutdown max wait  for the scheduler is even 30 minutes.
        scheduler.stop();
      } catch (Throwable t) {
        log.error("Failed to shutdown Kagkarlsson scheduler.", t);
      } finally {
        canShutDown = true;
      }
    }).start();
  }

  @Override
  public boolean canShutdown() {
    return canShutDown;
  }
}
