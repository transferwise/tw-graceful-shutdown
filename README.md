# TW Graceful Shutdown

This will keep your service running and kicking, until all the clients have understood that the node is going to shut down. 
It will make sure that all on-flight requests will get served, all the jobs will gracefully shut down, all kafka listeners will stop and so on and on.
The benefit is less errors for clients, but also cleaner Rollbar.

### How it works

1. GracefulShutdowner receives a stop notification.
1. It calls `prepareForShutdown` for reach GracefulShutdownStrategy
1. Waits for [30 secs](src/main/java/com/transferwise/boot/gracefulshutdown/config/GracefulShutdownProperties.java) so that clients understand that they should not call the node the anymore.
1. Waits until all strategies return true in the `canShutdown` method.

[Here](src/main/java/com/transferwise/boot/gracefulshutdown/strategies) is the list of default GracefulShutdownStrategy. 

You can add your own by just creating a bean that implements GracefulShutdownStrategy.

### Configurations

You can find the list of properties [here](src/main/java/com/transferwise/boot/gracefulshutdown/config/GracefulShutdownProperties.java).
