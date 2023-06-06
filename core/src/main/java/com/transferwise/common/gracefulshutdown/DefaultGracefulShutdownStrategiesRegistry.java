package com.transferwise.common.gracefulshutdown;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class DefaultGracefulShutdownStrategiesRegistry implements GracefulShutdownStrategiesRegistry {

  @Autowired
  @Getter
  @Setter
  private List<GracefulShutdownStrategy> strategies;

}