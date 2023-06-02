# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
