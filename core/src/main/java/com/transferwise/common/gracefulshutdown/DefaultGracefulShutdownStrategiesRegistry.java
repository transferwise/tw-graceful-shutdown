package com.transferwise.common.gracefulshutdown;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

@Slf4j
public class DefaultGracefulShutdownStrategiesRegistry implements GracefulShutdownStrategiesRegistry {

  @Autowired
  private ApplicationContext applicationContext;

  private List<GracefulShutdownStrategy> strategies;

  @Override
  public List<GracefulShutdownStrategy> getStrategies() {
    if (strategies == null) {
      List<GracefulShutdownStrategy> strategies = new ArrayList<>(applicationContext.getBeansOfType(GracefulShutdownStrategy.class).values());

      AnnotationAwareOrderComparator.sort(strategies);

      this.strategies = strategies;

      log.info("Following strategies were detected: '" + strategies + "'.");
    }
    return strategies;
  }
}