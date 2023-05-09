package com.transferwise.common.gracefulshutdown.test;

import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

// More customized scheduling is set up.
@SpringBootApplication
@EnableScheduling
public class TestBApplication {

  @Bean
  public SchedulingConfigurer mySchedulingConfigurer() {
    return taskRegistrar -> taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
  }
}
