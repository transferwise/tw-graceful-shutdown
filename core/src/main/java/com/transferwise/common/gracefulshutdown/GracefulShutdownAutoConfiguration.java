package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthIndicator;
import com.transferwise.common.gracefulshutdown.strategies.RequestCountGracefulShutdownStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "tw-graceful-shutdown.enable", matchIfMissing = true)
@EnableConfigurationProperties({GracefulShutdownProperties.class})
@AutoConfigureAfter(name = {"org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration"})
public class GracefulShutdownAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.AbstractHealthIndicator")
    protected static class HealthIndicatorConfiguration {
        @Bean
        @ConditionalOnProperty(value = "tw-graceful-shutdown.health-indicator.enabled", matchIfMissing = true)
        public GracefulShutdownHealthIndicator gracefulShutdownHealthIndicator() {
            return new GracefulShutdownHealthIndicator();
        }
    }

    @Configuration
    @RequiredArgsConstructor
    @EnableConfigurationProperties({RequestCountStrategyProperties.class})
    @ConditionalOnClass(name = "javax.servlet.Filter")
    protected static class ServletConfiguration {
        private final RequestCountStrategyProperties requestCountStrategyProperties;

        @Bean
        @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
        public FilterRegistrationBean<RequestCountGracefulShutdownStrategy> requestCountGracefulShutdownStrategyFilter() {
            FilterRegistrationBean<RequestCountGracefulShutdownStrategy> registrationBean = new FilterRegistrationBean<>();
            registrationBean.setFilter(requestCountGracefulShutdownStrategy());
            registrationBean.setOrder(requestCountStrategyProperties.getFilterOrder());
            return registrationBean;
        }

        @Bean
        @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
        public RequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy() {
            return new RequestCountGracefulShutdownStrategy();
        }
    }

    @Bean
    public GracefulShutdowner gracefulShutdowner() {
        return new GracefulShutdowner();
    }

}
