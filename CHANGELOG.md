## Version 0.41.4
#### Changed
* [#44](https://github.com/AvanzaBank/astrix/issues/44) Reduced string allocations for improved performance

## Version 0.41.0
#### Added
* [#31](https://github.com/AvanzaBank/astrix/issues/31) Relevant Hystrix metrics are exported as mbeans.

#### Changed
* [#28](https://github.com/AvanzaBank/astrix/issues/28) Merges BeanConfigurations and AstrixConfig abstractions 
* [#29](https://github.com/AvanzaBank/astrix/issues/29) Removes getConfig from AstrixContext/AstrixApplicationContext
* [#33](https://github.com/AvanzaBank/astrix/issues/33) New design of FaultToleranceSpi

## Version 0.40.0
#### Added
* [#27](https://github.com/AvanzaBank/astrix/issues/27) Adds support to listen for underlying property changes of a DynamicConfig instance
* [#12](https://github.com/AvanzaBank/astrix/issues/12) More server-side metrics for each exported servcie

#### Changed
* [#12](https://github.com/AvanzaBank/astrix/issues/12) New design of MetricsSpi


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
