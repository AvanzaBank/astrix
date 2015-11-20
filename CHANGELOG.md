## Version 0.39.2
#### Fixes
* [#25](https://github.com/AvanzaBank/astrix/issues/25) AstrixBeanSettings uses a "." for as separator between bean type and qualifier

## Version 0.39.1
#### Fixes
* [#24](https://github.com/AvanzaBank/astrix/issues/24) Inconsistent configuration property name of AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD

## Version 0.39.0
#### Fixes
* [#18](https://github.com/AvanzaBank/astrix/issues/18) GS_REMOTING throws ServiceUnavailableException when space proxy is closed
* [#20](https://github.com/AvanzaBank/astrix/issues/20) Broadcast remote service invocation supports async methods with void return type
* [#22](https://github.com/AvanzaBank/astrix/issues/22) Partitioned Routing remote service invocation supports async methods with void return type

#### Changes (Compatibility breaking)
* [#23](https://github.com/AvanzaBank/astrix/issues/23) Change in design of BeanFaultTolerance. Each bean has a dedicated Hystrix thread pool and all configuration of the fault tolerance layer is done using AstrixBeanSettings.
  * Astrix uses a custom strategy to read Hystrix configuration (delegates to different `AstrixBeanSettings`)
  * Hystrix thread pool is configured on a "per bean" basis rather than on a "per ApiProvider" basis
  * `HystrixCommandNamingStrategy` introduces non-backwards-compatible change, see javadoc for details.
  * See issue [#23](https://github.com/AvanzaBank/astrix/issues/23) for more details

#### Other Pulls
* [#19](https://github.com/AvanzaBank/astrix/pull/19) Upgrades to Hystrix 1.4.20 and RxJava 1.0.14
