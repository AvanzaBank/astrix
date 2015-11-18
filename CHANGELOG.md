## Version 0.39.0-SNAPSHOT
#### Fixes
* [#18](https://github.com/AvanzaBank/astrix/issues/18) GS_REMOTING throws ServiceUnavailableException when space proxy is closed
* [#20](https://github.com/AvanzaBank/astrix/issues/20) Broadcast remote service invocation supports async methods with void return type
* [#22](https://github.com/AvanzaBank/astrix/issues/22) Partitioned Routing remote service invocation supports async methods with void return type

#### Changes (Compatibility breaking)
* [#23](https://github.com/AvanzaBank/astrix/issues/23) Change in Design for BeanFaultTolerance. Each bean has a dedicated Hystrix thread pool.
  * Astrix uses custom strategy to read Hystrix configuration. All Hystrix settings is treaed asa `AstrixBeanSetting`
  * Hystrix thread pool is configured on a "per bean" basis rather than on a "per ApiProvider" basis
  * `HystrixCommandNamingStrategy` introduces non-backwards-compatible change, see javadoc

#### Other Pulls
* [#19](https://github.com/AvanzaBank/astrix/pull/19) Upgrades to Hystrix 1.4.20 and RxJava 1.0.14
