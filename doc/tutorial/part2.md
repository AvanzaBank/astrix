# Part 2 - Service Binding
Astrix has an extendable service-binding mechanism using using the `AstrixServiceComponent` interface. As an api-developer and consumer, you never use the `AstrixServiceComponent` directly. Rather its used behind the sceenes by astrix to bind to bind to a provider of a given servcie. However, event if you don't intend to implement you own service-binding mechanism it's good to have knowledge about the inner workings of service binding.


### Service Binding
- AstrixServiceComponent
- DirectComponent
- GsComponent

* Introduce AstrixConfigApi
* Illustrate service-binding with AstrixConfigApi and DirectComponent
* Illustrate that a service must not be available when bean is created
* Illustrate waiting for library bean to be bound


### Service Registry (Service discovery)
- Dynamic service discovery
- Relies on service-binding using AstrixServiceComponent