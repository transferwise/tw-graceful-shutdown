package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import com.transferwise.common.gracefulshutdown.strategies.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "tw-boot.graceful-shutdown.enable", matchIfMissing = true)
@EnableConfigurationProperties({GracefulShutdownProperties.class, RequestCountStrategyProperties.class})
@AutoConfigureAfter(name = {"org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration"})
public class GracefulShutdownAutoConfiguration {
    @Autowired
    private RequestCountStrategyProperties requestCountStrategyProperties;

    @Bean
    @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.health-indicator.enabled", matchIfMissing = true)
    public GracefulShutdownHealthIndicator gracefulShutdownHealthIndicator() {
        return new GracefulShutdownHealthIndicator();
    }

    @Bean
    @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public FilterRegistrationBean requestCountGracefulShutdownStrategyFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(requestCountGracefulShutdownStrategy());
        registrationBean.setOrder(requestCountStrategyProperties.getFilterOrder());
        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public RequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy() {
        return new RequestCountGracefulShutdownStrategy();
    }

    @Bean
    public GracefulShutdowner gracefulShutdowner() {
        return new GracefulShutdowner();
    }

    @Configuration
    protected static class JmsShutdownConfiguration {
        @ConditionalOnClass(name = "org.springframework.jms.config.JmsListenerEndpointRegistry")
        @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.jms-strategy.enabled", matchIfMissing = true)
        @Bean
        public JmsGracefulShutdownStrategy jmsGracefulShutdownStrategy() {
            return new JmsGracefulShutdownStrategy();
        }
    }

    @Configuration
    protected static class QuartzShutdownConfiguration {
        @ConditionalOnBean(type = "org.quartz.Scheduler")
        @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.quartz-strategy.enabled", matchIfMissing = true)
        @Bean
        public QuartzGracefulShutdownStrategy quartzGracefulShutdownStrategy() {
            return new QuartzGracefulShutdownStrategy();
        }
    }

    @Configuration
    protected static class EurekaShutdownConfiguration {
        @ConditionalOnClass(name = "org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration")
        @ConditionalOnProperty(value = "tw-boot.graceful-shutdown.eureka-strategy.enabled", matchIfMissing = true)
        @Bean
        public EurekaGracefulShutdownStrategy eurekaGracefulShutdownStrategy() {
            return new EurekaGracefulShutdownStrategy();
        }
    }
}
