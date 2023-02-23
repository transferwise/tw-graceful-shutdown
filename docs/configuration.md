# Configuration

Here is the list of available properties:

| **Property Name**               | **Default Value** | **Explanation**                                                                                                                                                                             |
|---------------------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `shutdownTimeoutMs`             | 90,000ms          | Maximum number of milliseconds a resource shutting down via `BaseReactiveResourceShutdownStrategy` should take to shutdown.                                                                 |
| `clientsReactionTimeMs`         | 30,000ms          | How many milliseconds to wait after calling `prepareForShutdown` on each `GracefulShutdownStrategy` so that all clients understand that they should not call this application node anymore. |
| `strategiesCheckIntervalTimeMs` | 5,000ms           | How long to wait in milliseconds between checking the `canShutdown` flag of each `GracefulShutdownStrategy` being executed.                                                                 |
| `resourceCheckIntervalTimeMs`   | 250ms             | How long to wait in milliseconds between checking termination status of a resource shutting down via the `BaseReactiveResourceShutdownStrategy`.                                            |

To configure the properties, edit your `application-*.yml` file in your `resources` folder as such:
```yaml
tw-graceful-shutdown:
  shutdown-timeout-ms: <INTEGER>
  client-reaction-time-ms: <INTEGER>
  strategies-check-interval-time-ms: <INTEGER>
  resource-check-internal-time-ms: <INTEGER>
```

### Health Indicator
```yaml
tw-graceful-shutdown:
  health-indicator:
    enabled: [true|default:false]
```
If set to `true`, creates `GracefulShutdownHealthStrategy` and `GracefulShutdownHealthIndicator` beans conditionally on the presence of the `org.springframework.boot.actuate.health.AbstractHealthIndicator` class.

### Kagkarlsson DB Scheduler
```yaml
tw-graceful-shutdown:
  kagkarlsson-db-scheduler:
    enabled: [true|default:false]
```
If set to `true`, creates the `KagkarlssonDbScheduledTaskShutdownStrategy` bean conditionally on the presence of the `com.github.kagkarlsson.scheduler.Scheduler` class and bean. Used to gracefully shutdown tasks that are being executed by the scheduler.

### Spring Task Scheduler
```yaml
tw-graceful-shutdown:
  spring-task-scheduler:
    enabled: [true|default:false]
```

If set to `true`, creates the `TaskSchedulersGracefulShutdownStrategy` bean conditionally on the presence of the `org.springframework.scheduling.TaskScheduler` bean or if that bean is missing then it checks for the presence of the `org.springframework.scheduling.annotation.SchedulerAnnotationBeanPostProcessor` and `org.springframework.scheduling.annotation.SchedulingConfigurer` beans.

### Executor Service
```yaml
tw-graceful-shutdown:
  executor-service:
    enabled: [true|default:false]
```

If set to `true`, creates the `ExecutorServiceGracefulShutdownStrategy` bean conditionally on the presence of the `java.util.concurrent.ExecutorService` class.

### Request Count Strategy
```yaml
tw-graceful-shutdown:
  request-count-strategy:
    enabled: [true|default:false]
```

If set to `true`, creates the `FilterRegistrationBean<RequestCountGracefulShutdownStrategy` and `RequestCountGracefulShutdownStrategy` beans.

> All the aforementioned strategy implementations can be found [here](https://github.com/transferwise/tw-graceful-shutdown/tree/master/core/src/main/java/com/transferwise/common/gracefulshutdown/strategies).