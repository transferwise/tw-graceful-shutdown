package com.transferwise.common.gracefulshutdown.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "tw-graceful-shutdown", ignoreUnknownFields = false)
@Data
@SuppressWarnings("checkstyle:magicnumber")
public class GracefulShutdownProperties {
  @Data
  public static class FlagProperty {
    private boolean enabled;
  }

  private int shutdownTimeoutMs = 90_000;

  private int clientsReactionTimeMs = 30_000;

  private int strategiesCheckIntervalTimeMs = 5_000;

  private int resourceCheckIntervalTimeMs = 250;

  private FlagProperty healthIndicator;
  private FlagProperty requestCountStrategy;
  private FlagProperty kagkarlssonDbScheduler;
  private FlagProperty springTaskScheduler;
  private FlagProperty executorService;
}
