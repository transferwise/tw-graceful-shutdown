package com.transferwise.common.gracefulshutdown;

import java.util.List;

public interface GracefulShutdownStrategiesRegistry {

  /**
   * Provides all strategies.
   *
   * <p>Startup events are sent in the order in the list.
   *
   * <p>Shutdown events are sent in the reverse order.
   */
  List<GracefulShutdownStrategy> getStrategies();
}
