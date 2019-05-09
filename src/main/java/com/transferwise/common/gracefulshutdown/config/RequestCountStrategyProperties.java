package com.transferwise.common.gracefulshutdown.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "tw-graceful-shutdown.request-count-strategy", ignoreUnknownFields = false)
@Data
public class RequestCountStrategyProperties {
    private int filterOrder = Integer.MIN_VALUE;
    private boolean enabled = true;
}
