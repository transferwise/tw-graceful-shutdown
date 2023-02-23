# tw-graceful-shutdown Documentation
> A library to help keep your service alive until all clients become aware that your service is going to shut down.

## Table of Contents
* [How it Works](#how-it-works)
* [Integrating with the Library](integration.md)
* [Configuration](configuration.md)
* [Contribution Guide](contributing.md)

## How it Works

##### When application has started:

1. It calls `applicationStarted` for each `GracefulShutdownStrategy`.

##### When application is stopped:

1. `GracefulShutdowner` receives a stop notification.
2. It calls `prepareForShutdown` for each `GracefulShutdownStrategy`
3. Waits for [30 secs](https://github.com/transferwise/tw-graceful-shutdown/blob/master/core/src/main/java/com/transferwise/common/gracefulshutdown/config/GracefulShutdownProperties.java) so that all clients understand that they should not call this application node anymore.
4. Waits until all strategies return true in the `canShutdown` method.

[Here](https://github.com/transferwise/tw-graceful-shutdown/tree/master/core/src/main/java/com/transferwise/common/gracefulshutdown/strategies) is the list of default `GracefulShutdownStrategy` implementations.

You can add your own by just creating a bean that implements `GracefulShutdownStrategy`.