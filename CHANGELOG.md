# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.15.1] - 2024-12-05

### Changed
* Added support for Spring Boot 3.4.
* Dropped support for Spring Boot 3.2.

## [2.15.0] - 2024-10-30

### Added

* Environment specific configuration.
  Test environment has now configured automatically to have fast shutdown.
* Delay before we announce service as healthy.

## [2.14.5] - 2024-07-16

### Added

* Added support for spring boot 3.3

## [2.14.4] - 2024-04-10

### Fixed

* Check termination status for `TaskSchedulerRouter` instances correctly.

## [2.14.3] - 2024-02-22

### Changed

* Add support for Spring Boot 3.2 and bump some dependencies.
  * In Spring Boot 3.2 logic for assigning a scheduler for executing `@Scheduled` annotated methods changed, so needed to refactored auto configuration and shutdown logic of `TaskSchedulersGracefulShutdownStrategy` to always ask the `ScheduledTaskRegistrar` for the actual `TaskScheduler` before shutting it down.
  * Add support for `TaskSchedulerRouter` in `TaskSchedulersGracefulShutdownStrategy`

## [2.14.2] - 2023-07-28

### Added

* Support for Spring Boot 3.1

### Bumped

* Build against Spring Boot 3.0.6 --> 3.0.7
* Build against Spring Boot 2.7.11 --> 2.7.13
* Build against Spring Boot 2.6.14 --> 2.6.15

## [2.14.1] - 2023-06-07

### Fixed

* Potential circular dependencies between custom strategies and strategies' registry.
* A bug, where we tried to sort an immutable list, when ordering strategies.

## [2.14.0] - 2023-06-06

### Changed

- Changed they way how strategies are found.
  Now, if there are circular dependencies, the application context starts to fail.
  We will also validate, just in case, if the strategies set will change between bean initializations and application startup.

## [2.13.0] - 2023-05-31

### Changed

- Application events are not used anymore, and we rely just on `SmartLifeCycleBean`.
  Application events based code was needed for some reason in Spring Boot 1.3.
  Due to `spring-cloud` creating multiple application contexts or something like that.

## [2.12.0] - 2023-05-26
* Do not enable `ExecutorServiceGracefulShutdownStrategy` by default.
* Allow users to disable resources from being managed by graceful shutdown using `@GracefulShutdownIgnore` annotation

## [2.11.1] - 2023-05-18

### Fixed
* Fix for `UnboundConfigurationPropertiesException` when one of the autocofiguration conditional flag properties were set explicitly.
e.g. `health-indicator.enabled: true` or `request-count-strategy.enabled: true`

## [2.11.0] - 2023-05-04

### Added

* Support for Spring Boot 3

### Changed

* Matrix tests are now testing against:
  - Spring Boot 3.0
  - Spring Boot 2.7
  - Spring Boot 2.6

* Request Count filter starts rejecting new calls after client reaction timeout has passed.

### Removed

* Matrix tests for:
  - Spring Boot 2.5
  - Spring Boot 2.4

## [2.10.0] - 2023-02-09

### Changes

* Refactor TaskSchedulersGracefulShutdownStrategy. Now it wait for published tasks to finish and force shutdown on timeout.

## [2.9.0] - 2023-01-19

### Changes

* Added strategy for graceful shutdown of ExecutorServices


## [2.8.1] - 2022-12-27

### Changes

* Fix IllegalAccessException on shutting down Executors defined as private class


## [2.8.0] - 2022-11-10

### Changes

* Adds autocomplete support with spring boot configuration processor

## [2.7.0] - 2022-05-03

### Fixes

* Wrong counter handling prevented Scheduled Tasks strategy to announce that shutdown can continue. Basically in some scenarios the counter got a
  negative value failing ==0 check at the end.

## [2.6.0] - 2022-05-03

### Changes

* Supporting Spring scheduling graceful shutdown for more exotic configurations.

## [2.5.0] - 2021-08-12

### Changes

* Added strategy for graceful shutdown of spring task scheduler beans. The ones, handling Scheduled annotation, for example.

## [2.4.0] - 2021-08-12

### Changes

* Added strategy for graceful shutdown of db-scheduler beans

## [2.3.1] - 2021-08-12

### Changes

* use `compareAndSet` to avoid `stop` from executing twice in GracefulShutdowner

## [2.3.0] - 2021-05-28
### Changed
* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.
* Graceful shutdown strategies can override `ready` method to signal if they are ready to accept traffic.

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
