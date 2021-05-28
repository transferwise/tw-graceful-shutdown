# TW Graceful Shutdown

![Apache 2](https://img.shields.io/hexpm/l/plug.svg)
![Java 11](https://img.shields.io/badge/Java-11-blue.svg)
![Maven Central](https://badgen.net/maven/v/maven-central/com.transferwise.common/tw-graceful-shutdown-core)

This will keep your service running and kicking, until all the clients have understood that the node is going to shut down. 
It will make sure that all on-flight requests will get served, all the jobs will gracefully shut down, all kafka listeners will stop and so on and on.

The benefits are - less errors for clients, but also cleaner monitoring.

## How it works

##### When application has started:

1. It calls `applicationStarted` for each `GracefulShutdownStrategy`.

##### When application is stopped:

1. GracefulShutdowner receives a stop notification.
2. It calls `prepareForShutdown` for each `GracefulShutdownStrategy`
3. Waits for [30 secs](core/src/main/java/com/transferwise/common/gracefulshutdown/config/GracefulShutdownProperties.java) so that all clients understand that they should not call this application node anymore.
4. Waits until all strategies return true in the `canShutdown` method.

[Here](core/src/main/java/com/transferwise/common/gracefulshutdown/strategies) is the list of default `GracefulShutdownStrategy` implementations. 

You can add your own by just creating a bean that implements `GracefulShutdownStrategy`.

## Configurations

You can find the list of properties [here](core/src/main/java/com/transferwise/common/gracefulshutdown/config/GracefulShutdownProperties.java).

## License
Copyright 2021 TransferWise Ltd.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
