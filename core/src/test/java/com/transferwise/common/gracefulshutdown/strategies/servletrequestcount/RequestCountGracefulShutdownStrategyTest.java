package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;


@EnabledIf("hasJakartaServletApi")
class RequestCountGracefulShutdownStrategyTest {

  protected static boolean hasJakartaServletApi() {
    try {
      Class.forName("jakarta.servlet.FilterChain");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Test
  void permission_for_shutdown_is_correctly_given_javax() {
    var strategy = new RequestCountGracefulShutdownStrategy();

    strategy.currentRequestsCount.incrementAndGet();
    assertThat(strategy.canShutdown()).isFalse();
    strategy.currentRequestsCount.decrementAndGet();
    assertThat(strategy.canShutdown()).isTrue();
  }
}
