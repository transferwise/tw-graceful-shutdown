package com.transferwise.common.gracefulshutdown.config;

import com.wise.common.environment.WiseEnvironment;
import com.wise.common.environment.WiseProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
public class GracefulShutdownEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

    WiseEnvironment.setDefaultProperties(dsl -> dsl
        .source("tw-graceful-shutdown")

        .keyPrefix("tw-graceful-shutdown.")
        .profile(WiseProfile.WISE)
        .set("startupHealthyDelayMs", 0)

        .keyPrefix("tw-graceful-shutdown.")
        .profile(WiseProfile.PRODUCTION)
        .set("startupHealthyDelayMs", 7500)

        .keyPrefix("tw-graceful-shutdown.")
        .profile(WiseProfile.TEST)
        .set("startupHealthyDelayMs", 1)
        .set("shutdown-timeout-ms", 1)
        .set("clients-reaction-time-ms", 1)
        .set("strategies-check-interval-time-ms", 5)
    );
  }

}
