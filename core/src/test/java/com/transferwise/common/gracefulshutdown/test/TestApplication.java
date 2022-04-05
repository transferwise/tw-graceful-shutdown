package com.transferwise.common.gracefulshutdown.test;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class TestApplication {

  public AtomicLong schedulerCounter = new AtomicLong();

  @Scheduled(fixedDelay = 5)
  public void doSomethingFrequently() {
    schedulerCounter.incrementAndGet();
  }

}
