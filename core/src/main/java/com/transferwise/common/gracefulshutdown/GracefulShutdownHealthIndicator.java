package com.transferwise.common.gracefulshutdown;

import com.google.common.util.concurrent.RateLimiter;
import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

@Slf4j
public class GracefulShutdownHealthIndicator extends AbstractHealthIndicator {

  @Autowired
  private GracefulShutdownStrategiesRegistry registry;

  @Autowired
  private GracefulShutdownHealthStrategy healthStrategy;

  private RateLimiter logsRateLimiter = RateLimiter.create(0.2);

  private boolean ready;

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    if (!ready) {
      for (GracefulShutdownStrategy strategy : registry.getStrategies()) {
        if (!strategy.isReady()) {
          if (logsRateLimiter.tryAcquire()) {
            log.info("" + strategy + " is not ready. Not considering application ready yet.");
          }
          builder.down();
          return;
        }
      }
      ready = true;
      log.info("All strategies are reporting ready.");
    }

    if (healthStrategy.isShutdownInProgress()) {
      builder.down();
      return;
    }

    builder.up();
  }

}
