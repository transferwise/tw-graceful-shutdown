package com.transferwise.common.gracefulshutdown.strategies;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RequestCountGracefulShutdownStrategyTest {

  @Test
  public void permission_for_shutdown_is_correctly_given() {
    RequestCountGracefulShutdownStrategy strategy = new RequestCountGracefulShutdownStrategy();
    strategy.requestCount.incrementAndGet();
    assertThat(strategy.canShutdown()).isFalse();
    strategy.requestCount.decrementAndGet();
    assertThat(strategy.canShutdown()).isTrue();
  }

}
