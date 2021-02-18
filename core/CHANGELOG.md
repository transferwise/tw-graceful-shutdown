# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2021-02-17
### Changed
* Strategies are now executed in order specified by Spring's bean ordering system.
Please consult with `org.springframework.core.annotation.Order` how you can specify that order.
  
`applicationStarted` hooks for strategies are called in the Spring's order.
Shutdown hooks for strategies are called in the reverse order.

Custom order can be used as well, by overriding `DefaultGracefulShutdownStrategiesRegistry` bean.

## [2.0.0] - 2020-12-30
### Changed
* Spring Boot version from 2.1.5.RELEASE to 2.3.7.RELEASE

### Removed
* Spring Cloud
* EurekaGracefulShutdownStrategy
* QuartzGracefulShutdownStrategy

## [1.2.7] - 2020-08-13
### Changed
* The auto configurator can now work when `javax.servlet.Filter` is not in classpath.
* The auto configurator can now work when `org.springframework.boot.actuate.health.AbstractHealthIndicator` is not in classpath.
