package com.transferwise.common.gracefulshutdown.test;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class StrategyExecutionOrderCollector {

  @Getter
  private List<String> applicationStartedList = new ArrayList<>();
  @Getter
  private List<String> canShutdownList = new ArrayList<>();

  public void applicationStarted(String strategyName) {
    applicationStartedList.add(strategyName);
  }

  public void canShutdown(String strageyName) {
    canShutdownList.add(strageyName);
  }
}
