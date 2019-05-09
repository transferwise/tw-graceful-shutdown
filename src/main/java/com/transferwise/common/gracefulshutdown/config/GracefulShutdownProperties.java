package com.transferwise.common.gracefulshutdown.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tw-graceful-shutdown")
@Data
@SuppressWarnings("checkstyle:magicnumber")
public class GracefulShutdownProperties {
    private int shutdownTimeoutMs = 60000;

    private int clientsReactionTimeMs = 30000;

    private int strategiesCheckIntervalTimeMs = 5000;
}
