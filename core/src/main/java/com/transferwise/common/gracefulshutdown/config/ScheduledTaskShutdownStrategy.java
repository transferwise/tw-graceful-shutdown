package com.transferwise.common.gracefulshutdown.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
@ConditionalOnClass(name = "com.github.kagkarlsson.scheduler.Scheduler")
@ConditionalOnProperty(value = "tw-graceful-shutdown.db-scheduler.enabled", matchIfMissing = true)
public class ScheduledTaskShutdownStrategy implements GracefulShutdownStrategy {
  private final Scheduler scheduler;

  @Override
  public void prepareForShutdown() {
    log.info("Attempting to shutdown scheduler");
    scheduler.stop();
  }

  @Override
  public boolean canShutdown() {
    return scheduler.getCurrentlyExecuting().isEmpty();
  }
}
