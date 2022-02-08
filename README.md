<p align="center"><img width=50% src="https://raw.githubusercontent.com/DataCloud-project/toolbox/master/docs/img/datacloud_logo.png"></p>&nbsp;

[![GitHub Issues](https://img.shields.io/github/issues/DataCloud-project/R-MARKET_Worker.svg)](https://github.com/DataCloud-project/R-MARKET_Worker/issues)
[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# R-MARKET Worker
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
