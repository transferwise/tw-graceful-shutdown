package com.transferwise.common.gracefulshutdown.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class TestApplication {

  @Scheduled(fixedDelay = 5)
  public void doSomethingFrequently() {
    // something
  }

}
