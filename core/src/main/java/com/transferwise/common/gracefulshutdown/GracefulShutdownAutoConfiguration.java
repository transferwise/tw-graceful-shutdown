package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.gracefulshutdown.config.GracefulShutdownProperties;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import com.transferwise.common.gracefulshutdown.strategies.ExecutorServiceGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthStrategy;
import com.transferwise.common.gracefulshutdown.strategies.KagkarlssonDbScheduledTaskShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.TaskSchedulersGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.LegacyRequestCountGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.RequestCountGracefulShutdownStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

@Configuration
@ConditionalOnProperty(value = "tw-graceful-shutdown.enable", matchIfMissing = true)
@EnableConfigurationProperties({GracefulShutdownProperties.class})
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class GracefulShutdownAutoConfiguration {

  @Configuration
  @ConditionalOnClass(name = "org.springframework.boot.actuate.health.AbstractHealthIndicator")
  protected static class HealthIndicatorConfiguration {

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.health-indicator.enabled", matchIfMissing = true)
    public GracefulShutdownHealthStrategy gracefulShutdownHealthStrategy() {
      return new GracefulShutdownHealthStrategy();
    }

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.health-indicator.enabled", matchIfMissing = true)
    public GracefulShutdownHealthIndicator gracefulShutdownHealthIndicator() {
      return new GracefulShutdownHealthIndicator();
    }
  }

  // To be removed after we drop support for Spring Boot 2
  @Configuration
  @RequiredArgsConstructor
  @EnableConfigurationProperties({RequestCountStrategyProperties.class})
  @ConditionalOnClass(name = "javax.servlet.Filter")
  protected static class LegacyServletConfiguration {

    private final RequestCountStrategyProperties requestCountStrategyProperties;

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public FilterRegistrationBean legacyRequestCountGracefulShutdownStrategyFilter(
        LegacyRequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy) throws Exception {
      var registrationBean = new FilterRegistrationBean();

      var m = registrationBean.getClass().getMethod("setFilter", Class.forName("javax.servlet.Filter"));
      m.invoke(registrationBean, requestCountGracefulShutdownStrategy);
      registrationBean.setOrder(requestCountStrategyProperties.getFilterOrder());
      return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public LegacyRequestCountGracefulShutdownStrategy legacyRequestCountGracefulShutdownStrategy() {
      return new LegacyRequestCountGracefulShutdownStrategy();
    }
  }

  @Configuration
  @RequiredArgsConstructor
  @EnableConfigurationProperties({RequestCountStrategyProperties.class})
  @ConditionalOnClass(name = "jakarta.servlet.Filter")
  protected static class ServletConfiguration {

    private final RequestCountStrategyProperties requestCountStrategyProperties;

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public FilterRegistrationBean requestCountGracefulShutdownStrategyFilter(
        RequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy) throws Exception {
      var registrationBean = new FilterRegistrationBean();

      var m = registrationBean.getClass().getMethod("setFilter", Class.forName("jakarta.servlet.Filter"));
      m.invoke(registrationBean, requestCountGracefulShutdownStrategy);
      registrationBean.setOrder(requestCountStrategyProperties.getFilterOrder());

      return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "tw-graceful-shutdown.request-count-strategy.enabled", matchIfMissing = true)
    public RequestCountGracefulShutdownStrategy requestCountGracefulShutdownStrategy() {
      return new RequestCountGracefulShutdownStrategy();
    }
  }

  @Configuration
  @ConditionalOnClass(name = "com.github.kagkarlsson.scheduler.Scheduler")
  @ConditionalOnProperty(value = "tw-graceful-shutdown.kagkarlsson-db-scheduler.enabled", matchIfMissing = true)
  @ConditionalOnBean(com.github.kagkarlsson.scheduler.Scheduler.class)
  protected static class KagkarlssonDbScheduledConfiguration {

    @Bean
    public KagkarlssonDbScheduledTaskShutdownStrategy kagkarlssonDbScheduledTaskShutdownStrategy() {
      return new KagkarlssonDbScheduledTaskShutdownStrategy();
    }
  }

  @Configuration
  @ConditionalOnBean(type = {"org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor"})
  @ConditionalOnProperty(value = "tw-graceful-shutdown.spring-task-scheduler.enabled", matchIfMissing = true)
  protected static class SpringTaskSchedulerConfiguration {

    @Bean
    public TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy(
        @Autowired ApplicationContext applicationContext,
        @Autowired GracefulShutdownProperties gracefulShutdownProperties) {
      return new TaskSchedulersGracefulShutdownStrategy(applicationContext, gracefulShutdownProperties);
    }

    @Bean
    public SchedulingConfigurer twGsSchedulingConfigurer(TaskSchedulersGracefulShutdownStrategy taskSchedulersGracefulShutdownStrategy) {
      return taskSchedulersGracefulShutdownStrategy::setTaskRegistrar;
    }

  }

  @Configuration
  @ConditionalOnClass(name = "java.util.concurrent.ExecutorService")
  @ConditionalOnProperty(value = "tw-graceful-shutdown.executor-service.enabled", matchIfMissing = false)
  @ConditionalOnBean(java.util.concurrent.ExecutorService.class)
  protected static class ExecutorServiceGracefulShutdownStrategyConfiguration {

    @Bean
    public ExecutorServiceGracefulShutdownStrategy executorServiceGracefulShutdownStrategy(
        @Autowired ApplicationContext applicationContext,
        @Autowired GracefulShutdownProperties gracefulShutdownProperties
    ) {
      return new ExecutorServiceGracefulShutdownStrategy(applicationContext, gracefulShutdownProperties);
    }
  }

  @Bean
  @ConditionalOnMissingBean
  public GracefulShutdowner gracefulShutdowner() {
    return new GracefulShutdowner();
  }

  @Bean
  @ConditionalOnMissingBean
  public GracefulShutdownStrategiesRegistry gracefulShutdownStrategiesRegistry() {
    return new DefaultGracefulShutdownStrategiesRegistry();
  }


}
