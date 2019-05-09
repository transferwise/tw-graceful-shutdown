package com.transferwise.common.gracefulshutdown.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tw-boot.graceful-shutdown.request-count-strategy")
@Data
public class RequestCountStrategyProperties {
    private int filterOrder = Integer.MIN_VALUE;
    private boolean enabled = true;
}
