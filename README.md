# Asterix
Asterix is a framework designed to simplify development and maintenance of microservices. It is used by service providers to publish provided services, and by service consumers to bind to those services. Most applications do both, they provide services as well as consume other services.

Some of the features provided:
- service registration and discovery
- service binding
- service versioning
- fault tolerance

Asterix designed to support an organization where many teams develop different services and make those services available for other teams using Asterix.[REMOVE? Asterix simplifies development of micro services as well as maintenance by limiting what is exposed to service consumers. That is, it gives the service provider full control over what is exposed to its consumers, and what constitutes an internal implementation detail.]

## Service Registry
Service registration and discovery is done using the service registry. It is an application that allows service-providers to register all services they provide and by service-consumers to discover providers of given services.

## Service Binding
One of the main responsibilities for Asterix is service binding, which is done in three steps:

1. Asterix discovers a provider of a given service, typically using the service-registry
2. Asterix uses information retrieved from discovery to decide what mechanism to use to bind to the given service provider. In Asterix that binding mechanism is called `ServiceComponent`
3. Asterix uses the ServiceComponent to bind to the given provider

It's also possible to locate providers without using the service-registry, for instance using configuration. The service-registry itself is located using the configuration mechanism.

If a service is discovered using the service-registry, then a lease-manager thread will run for the given service in the background. The lease-manager will periodically ask the service-registry for information about where the given service is located, and if the service has moved the lease-manager will rebind the to the new provider.

## Service Versioning
A key goal of Asterix is to support independent release cycles of different microservices. To achieve that Asterix has built in support for data-format versioning which allows a service provider to serve clients that invokes the service using an old version of the client. Asterix uses a migration framework to upgrade incoming requests and downgrade outgoing responses to the version used by the given client. 



### Versioning framework design
1. The service implementation only knows the latest version of the data-format
2. The service-provider implement ”migrations” for each change in data-foramt which upgrade/downgrade a message from one version to the next
3. Asterix uses the migrations in all message-exchanges (service invocations) to upgrade an incoming requests to the latest version before invoking the actual service implementation, and downgrade responses before sending them back to the client.

### Service versioning workflow

## Easy testing of Microservices
Another big benefit of using Asterix is that it increases testability of micro services a lot. 

- Unittesting libraries
- Component testing microservices

## Fault tolerance
Asterix uses a fault-tolerance layer built using Hystrix. Depending on the type of service consumed, Asterix will decide what isolation mechanism to use and protect each service-invocation using the given mechanism.

## Spring Integration
Asterix is well integrated with spring. It can be viewed as an extension to spring where Asterix is responsible for binding and creating spring-beans for microservices.

## Generic module layout
Typical module-layout when creating a service using Asterix: 
* API
* API provider
* Server

## Example - The LunchApi

Modules
* lunch-api
* lunch-api-provider
* lunch-server 


### API (lunch-api) 
```java
interface LunchService {
	@AsterixBroadcast(reducer = LunchSuggestionReducer.class)
	LunchRestaurant suggestRandomLunchRestaurant(String foodType);
}

interface LunchRestaurantAdministrator {
	void addLunchRestaurant(LunchRestaurant restaurant);
}

interface LunchRestaurantGrader {
	void grade(@Routing String restaurantName, int grade);
	double getAvarageGrade(@Routing String restaurantName);
}
```

### API descriptor (lunch-api-provider)

```java
// The API is versioned.
@AsterixVersioned(
	apiMigrations = {
		LunchApiV1Migration.class
	},	
	version = 2,
	objectMapperConfigurer = LunchApiObjectMapperConfigurer.class
)
// The service is exported to the service-registry. Service is bound by Asterix at runtime using service-registry
@AsterixServiceRegistryApi(
	exportedApis = {
		LunchService.class,
		LunchAdministrator.class,
		LunchRestaurantGrader.class
	}
)
public class LunchApiDescriptor {
}
```

### Migration (lunch-api-provider)

```java
public interface AsterixJsonApiMigration {
	
	int fromVersion();
	
	AsterixJsonMessageMigration<?>[] getMigrations();

}

public class LunchApiV1Migration implements AsterixJsonApiMigration {

	@Override
	public int fromVersion() {
		return 1;
	}
	
	@Override
	public AsterixJsonMessageMigration<?>[] getMigrations() {
		return new AsterixJsonMessageMigration[] {
			new LunchRestaurantV1Migration()
		};
	}
	
	private static class LunchRestaurantV1Migration implements AsterixJsonMessageMigration<LunchRestaurant> {

		@Override
		public void upgrade(ObjectNode json) {
			json.put("foodType", "unknown");
		}
		
		@Override
		public void downgrade(ObjectNode json) {
			json.remove("foodType");
		}

		@Override
		public Class<LunchRestaurant> getJavaType() {
			return LunchRestaurant.class;
		}
	}
}
```

## Providing the lunch-api

### Service implementations

```java
@AsterixServiceExport({LunchService.class, InternalLunchFeeder.class})
public class LunchServiceImpl implements LunchService, InternalLunchFeeder {
}

// And other service-implementations
```

### Service Descriptor

```java
@AsterixService(
	apiDescriptors = {
		LunchApiDescriptor.class,
		LunchFeederApiDescriptor.class
	},
	subsystem = "lunch-service",
	component = AsterixServiceComponentNames.GS_REMOTING
)
public class LunchServiceDescriptor {
}
```

### pu.xml

```xml
<!-- Asterix service framework (provider and consumer) -->
<bean id="asterixFrameworkBean" class="com.avanza.asterix.context.AsterixFrameworkBean">
	<property name="serviceDescriptor" value="com.avanza.asterix.integration.tests.domain.apiruntime.LunchServiceDescriptor"/>
</bean>

<!-- The actual service implementation(s) -->
<bean id="lunchService" class="com.avanza.asterix.integration.tests.domain.pu.LunchServiceImpl"/>
```



## Consuming the lunch-api

### pu.xml (or an ordinary spring.xml)

```xml
<bean id="asterixFrameworkBean" class="com.avanza.asterix.context.AsterixFrameworkBean">
	<property name="consumedAsterixBeans">
		<list>
			<value>com.avanza.asterix.integration.tests.domain.api.LunchService</value>
		</list>
	</property>
</bean>
```