package com.transferwise.common.gracefulshutdown.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "tw-graceful-shutdown.request-count-strategy", ignoreUnknownFields = false)
@Data
public class RequestCountStrategyProperties {

  private int filterOrder = Integer.MIN_VALUE;
  private boolean enabled = true;
  private Set<String> ignoredUris = new HashSet<>(Arrays.asList(
      "/health", "/actuator/health", "/prometheus", "/actuator/prometheus",
      "/liveness", "/actuator/liveness"));
}
