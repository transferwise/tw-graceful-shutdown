package com.transferwise.common.gracefulshutdown.test;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseOrderStrategy implements GracefulShutdownStrategy {

  @Autowired
  private StrategyExecutionOrderCollector strategyExecutionOrderCollector;

  protected String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void applicationStarted() {
    strategyExecutionOrderCollector.applicationStarted(getName());
  }

  @Override
  public boolean canShutdown() {
    strategyExecutionOrderCollector.canShutdown(getName());
    return true;
  }
}
