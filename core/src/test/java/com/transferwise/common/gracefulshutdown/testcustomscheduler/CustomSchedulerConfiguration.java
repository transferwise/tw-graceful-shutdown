package com.transferwise.common.gracefulshutdown.testcustomscheduler;

import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

// More customized scheduling is set up.
@Configuration
public class CustomSchedulerConfiguration {

  @Bean
  public SchedulingConfigurer mySchedulingConfigurer() {
    return taskRegistrar -> taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
  }
}
