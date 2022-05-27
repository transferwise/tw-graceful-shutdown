package com.transferwise.common.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.gracefulshutdown.test.StrategyExecutionOrderCollector;
import com.transferwise.common.gracefulshutdown.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Force a separate ApplicationContext, because we will close this one.
@ActiveProfiles({"test", "shutdown"})
@SpringBootTest(classes = {TestApplication.class})
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
