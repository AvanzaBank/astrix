# Part 3 - The Service Registry

TODO:

### Service Binding
* GsComponent

### Service Registry (Service discovery)
* Dynamic service discovery
* Relies on service-binding using AstrixServiceComponent

### Service versioning
* Migration framework
* Subsystem boundaries
* Exception handling (All service exceptions are wrapped in AstrixRemoteServiceException, logged on server, correlationId)

### Fault tolerance plugin
* Hystrix