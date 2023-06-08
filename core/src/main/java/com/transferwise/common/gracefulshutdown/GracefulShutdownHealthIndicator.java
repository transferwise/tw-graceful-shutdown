package com.transferwise.common.gracefulshutdown;

import com.google.common.util.concurrent.RateLimiter;
import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Lazy;

@Slf4j
public class GracefulShutdownHealthIndicator extends AbstractHealthIndicator {

  /*
   * We need to access the registry in a lazy way, otherwise we may easily run into circular dependencies with other strategies.
   * 
   * E.g. some other strategy may inject something needing actuators, which in turn triggers creating the health indicators.
   * 
   * We could avoid this by for example sending out the list of all strategies in some callback method, e.g. "applicationStarted",
   * but that kind of solution seems a bit of over-engineering.
   */
  @Lazy
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
