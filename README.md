# R-MARKET_Worker
This component is influenced and developed based on the [iexec-worker](https://github.com/iExecBlockchainComputing/iexec-worker).

### Overview

This component is in charge of running computing tasks sent by requesters through the Marketplace.


### Run an R-MARKET_Worker


#### With Gradle

*Please first update your config located in `./src/main/resources/application.yml`*

* for dev purposes:

```
cd iexec-worker
gradle bootRun --refresh-dependencies
```
* or on a remote instance:
```
cd iexec-worker
./gradlew bootRun --refresh-dependencies
```
