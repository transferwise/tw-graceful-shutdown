package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.gracefulshutdown.test.BaseTestEnvironment;
import com.transferwise.common.gracefulshutdown.test.StrategyExecutionOrderCollector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@BaseTestEnvironment
// Force a separate ApplicationContext, because we will close this one.
@ActiveProfiles({"test", "shutdown"})
public class StrategyExecutionOrderIntTest {

  @Autowired
  private GracefulShutdowner gracefulShutdowner;
  @Autowired
  private StrategyExecutionOrderCollector strategyExecutionOrderCollector;

  @Test
  void strategiesExecutionOrderIsCorrect() {
    assertThat(strategyExecutionOrderCollector.getApplicationStartedList()).containsExactly("StrategyB", "StrategyA", "StrategyC");

    gracefulShutdowner.stop();
    assertThat(strategyExecutionOrderCollector.getCanShutdownList()).containsExactly("StrategyC", "StrategyA", "StrategyB");
  }

}
