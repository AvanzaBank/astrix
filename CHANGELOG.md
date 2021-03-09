## Version 1.0.0
This new major version release is primarily including version updates of central, underlying technologies in Astrix; Such as Java, GigaSpaces and Spring versions.
### Changed 

* Updated to Java `11`
* Updated to GigaSpaces `14.5.0`
* Updated Spring Framework to version `5.1.7`
* Updated Apache Log4j to version `2.13.3`

## Version 0.45.5
### Added
* [#88](https://github.com/AvanzaBank/astrix/pull/88) TP-559: Log server and client apis and impls [[7185ea3]](https://github.com/AvanzaBank/astrix/commit/7185ea36f807edadc0db7bb9574da68630bb7857)
### Changed
* [#91](https://github.com/AvanzaBank/astrix/pull/91) TP-322: Apply contextpropagators on async callbacks [[fd90b04]](https://github.com/AvanzaBank/astrix/commit/fd90b04ffb8f2ccbafc89bccdf94ae02a5d02a98)

## Version 0.45.4
### Changed
* [#78](https://github.com/AvanzaBank/astrix/pull/78) Specify explicit version in mvn `<relocation>` [[36a716a]](https://github.com/AvanzaBank/astrix/commit/36a716ada3885b2b81eafcec1dcc51962721b588)


## Version 0.45.3
### Changed
* [#76](https://github.com/AvanzaBank/astrix/pull/76) TP-384: Use `mimer-config` instead of `astrix-config` [[8bbc83d]](https://github.com/AvanzaBank/astrix/commit/8bbc83d813b39e8c17b115d353702d9af67f0ad2) 
* [#74](https://github.com/AvanzaBank/astrix/pull/74) TP-431: Use explicit groupname for gs-test [[97bd498]](https://github.com/AvanzaBank/astrix/commit/97bd4985fe36ff56f30c9cd0c6d1b4fa1d84d9ed) 
* [#72](https://github.com/AvanzaBank/astrix/pull/72) TP-431: Remove astrix-gs-test-util and use gs-test instead [[6eb6598]](https://github.com/AvanzaBank/astrix/commit/6eb659841b7804a0e15a4779fdcff9234d39c062) 

## Version 0.45.2
### Changed
* [#70](https://github.com/AvanzaBank/astrix/pull/70) TP-415: Always run `maven-source-plugin` [[b0bb1f3]](https://github.com/AvanzaBank/astrix/commit/b0bb1f3d1835a659f03800ed8108ba58a986d2c4)

## Version 0.45.1
### Changed
* [#68](https://github.com/AvanzaBank/astrix/pull/68) TP-325: Allow singleton CredentialsProvider during GS tests [[80521d8]](https://github.com/AvanzaBank/astrix/commit/80521d8b63cadba24e0c4836391008c70388971c)

## Version 0.45.0
### Added
* [#66](https://github.com/AvanzaBank/astrix/pull/66) TP-325: Allow Astrix plugins for server- & client authentication [[d72ae0c]](https://github.com/AvanzaBank/astrix/commit/d72ae0cebcd2b2a5c7d17e4d79240d4fa3079ac3)

## Version 0.44.2
### Changed
* [#65](https://github.com/AvanzaBank/astrix/pull/65) TP-349: Allow building on Java11 [[fc3bf6a]](https://github.com/AvanzaBank/astrix/commit/fc3bf6aa1ecab38c92a22a15e1cd6dd527563dc6)


## Version 0.44.1
### Fixed
* [#61](https://github.com/AvanzaBank/astrix/pull/61) TP-270: Bugfix: Initialize `ClusteredProxyCacheImpl` with known classes [[72f9731]](https://github.com/AvanzaBank/astrix/commit/72f97314c6100cd6a958ac2504c06698e9e60006)

## Version 0.44.0
### Added
* [#59](https://github.com/AvanzaBank/astrix/pull/59) Added Astrix Tracing [[5e2ef48]](https://github.com/AvanzaBank/astrix/commit/5e2ef480ed71540cc7ee5dd2c28f8474c8de2005)

## Version 0.43.12
### Changed
* Updated rxjava version to `1.3.8` [[9fcb215]](https://github.com/AvanzaBank/astrix/commit/9fcb215bfa0146ae6707bfafbdccf53eaefaf423)

## Version 0.43.11
### Changed
* Updated rxjava version to `1.3.0` [[d2af7f7]](https://github.com/AvanzaBank/astrix/commit/d2af7f737f32fbad876311e645d6a60e12b167f4)
### Fixed
* Fixed typo [[fddf030]](https://github.com/AvanzaBank/astrix/commit/fddf030df8bb2b2bc48c372481ef6b718237c383)

## Version 0.43.10
### Added
* [#56](https://github.com/AvanzaBank/astrix/pull/56) Adds unit test for `AstrixServiceRegistryImpl::deregister` [[50b987b]](https://github.com/AvanzaBank/astrix/commit/762de43af38112aef84712b7e824b1e70592de60)
### Changed
* Updated rxjava version to `1.2.7` [[29c1e63]](https://github.com/AvanzaBank/astrix/commit/29c1e63119fdcfa55873717ff7c526511828598e)
### Removed
* Removed unused import of `com.google.common.base.Optional` [[0992ef7]](https://github.com/AvanzaBank/astrix/commit/0992ef73253ce7996a8aa1acb725a63a60dc483a) 
 

## Version 0.43.7
### Changed
* Updated rxjava version to `1.2.1` [[2f05bd5]](https://github.com/AvanzaBank/astrix/commit/2f05bd5e82ae182f88df56353c0299565f9a29ca)

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
