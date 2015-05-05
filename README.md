# Astrix
Astrix is a framework designed to simplify development and maintenance of microservices. At Avanza we use Astrix to run hundereds of microservices collaborating to provide the services required by the backend for the Avanza web site and our mobile applications.

Some of the features provided by Astrix are:

* service registration and discovery
* service binding
* service versioning
* fault tolerance

The design philosophy behind Astrix aligns well with the characteristics of microservices described by James Lewis and Martin Fowler (http://martinfowler.com/articles/microservices.html). However it's worth pointing out where the characteristics between a microservice developed using Astrix, and other microservice approaches differ:

* Selected centralized governance (as opposed to fully decentralized governance)
* Transport mechanism agnostic service consumers

## Selected centralized governance
Microservices are a broad field and Astrix is designed to emphasize certain characteristics of microservices, whereas other characterisitcs are intentionally ignored. Most notably Astrix does not emphasize an organization where different microservices are developed using completely different technology stacks. Quite opposite Astrix assumes a standardization using the jvm as the platform for running microservices. But, apart from standardizing on the jvm, microservice providers are free to use a technology stack most suitable for their needs. Most notably each microservice could choose a data-store suitable for their needs, and a programming language of their choice, as long as it runs on the jvm and can run java code.

At Avanza we have choosen to standardize on a single language (Java), a single application framework (GigaSpaces, which in turn is built on top of Spring), and a single service framework (Astrix). This is actually quite restricting compared to common view of microservices which often favor a decentralized governance.

## Transport mechanism agnostic service consumers
Although microservices are not by any mean tied to a given protocol, the common approach to microservices is to export "REST"-style services over http. This means that consumers of a service are tied to use http to access a given service, which is a severe limitation in testing scenarios. Service consumers using Astrix are completely decoupled from the transport mechanism used by a given service provider, which allows simple stubbing of consumed services allowing each micro service to be tested in isolation, without any special spring configuration files to stub out service dependencies. A typical microservice developed using Astrix can be started in-memory using the same spring configuration as in a production environment, but where the services consumed by the given application are completely stubbed out using in-memory mocks. The magic that allows this simple stubbing is provided by the service-registry and the dynamic service-binding mechanism provided by Astrix.


## Service Registry
Service registration and discovery is done using the service registry. It is an application that allows service-providers to register all services they provide and by service-consumers to discover providers of given services.

## Service Binding
One of the main responsibilities for Astrix is service binding, which is done in three steps:

1. Astrix discovers a provider of a given service, typically using the service-registry
2. Astrix uses information retrieved from discovery to decide what mechanism to use to bind to the given service provider. In Astrix that binding mechanism is called `AstrixServiceComponent`
3. Astrix uses the AstrixServiceComponent to bind to the given provider

It's also possible to locate providers without using the service-registry, for instance using configuration. The service-registry itself is located using the configuration mechanism.

If a service is discovered using the service-registry, then a lease-manager thread will run for the given service in the background. The lease-manager will periodically ask the service-registry for information about where a given service is located, and if the service has moved the lease-manager will rebind the to the new provider.

## Service Versioning
A key goal of Astrix is to support independent release cycles of different microservices. To achieve that Astrix has built in support for data-format versioning which allows a service provider to serve clients that invokes the service using an old version of the client. Astrix uses a migration framework to upgrade incoming requests and downgrade outgoing responses to the version requested by the given client. 



### Versioning framework design
1. The service implementation only knows the latest version of the data-format
2. The service-provider implement ”migrations” for each change in data-foramt which upgrade/downgrade a message from one version to the next
3. Astrix uses the migrations in all message-exchanges (service invocations) to upgrade an incoming requests to the latest version before invoking the actual service implementation, and downgrade the response received from the service implementation before sending them back to the client.

### Service versioning workflow

## Easy testing of Microservices
Another big benefit of using Astrix is that it increases testability of micro services a lot. 

- Unittesting libraries
- Component testing microservices

## Fault tolerance
Astrix uses a fault-tolerance layer built using Hystrix. Depending on the type of service consumed, Astrix will decide what isolation mechanism to use and protect each service-invocation using the given mechanism.

## Spring Integration
Astrix is well integrated with spring. It can be viewed as an extension to spring where Astrix is responsible for binding and creating spring-beans for microservices.


## Documentation
[Tutorial](doc/tutorial/index.md)


## Generic module layout
Typical module-layout when creating a service using Astrix: 
* API
* API provider
* Server

## Example - The LunchApi

Modules
* lunch-api
* lunch-api-provider
* lunch-server 


### API (located in lunch-api) 
```java
interface LunchService {
	@AstrixBroadcast(reducer = LunchSuggestionReducer.class)
	LunchRestaurant suggestRandomLunchRestaurant(String foodType);
}

interface LunchRestaurantAdministrator {
	void addLunchRestaurant(LunchRestaurant restaurant);
}

interface LunchRestaurantGrader {
	void grade(@AstrixRouting String restaurantName, int grade);
	double getAvarageGrade(@AstrixRouting String restaurantName);
}
```

### API provider (lunch-api-provider)

```java
// The API is versioned.
@AstrixObjectSerializerConfig(
	version = 2,
	objectSerializerConfigurer = LunchApiObjectSerializerConfigurer.class
)
// The service is exported to the service-registry. Consumers queries the service-registry to bind to servers.
@AstrixApiProvider
public interface LunchServiceProvider {
	
	@Versioned
	@Service
	LunchService lunchService();

	@Versioned
	@Service
	LunchRestaurantAdministrator lunchRestaurantAdministrator();

	@Versioned
	@Service
	LunchRestaurantGrader lunchRestaurantGrader();
}
```

### Object Serializer Configurer (lunch-api-provider)
```java
public class LunchApiObjectSerializerConfigurer implements Jackson1ObjectSerializerConfigurer {
	
	@Override
	public List<? extends AstrixJsonApiMigration> apiMigrations() {
		return Arrays.asList(new LunchApiV1Migration());
	}

	@Override
	public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
		// No custom configuration of Jackson required.
	}

}
```

### Migration (lunch-api-provider)
```java

public class LunchApiV1Migration implements AstrixJsonApiMigration {

	@Override
	public int fromVersion() {
		return 1;
	}
	
	@Override
	public AstrixJsonMessageMigration<?>[] getMigrations() {
		return new AstrixJsonMessageMigration[] {
			new LunchRestaurantV1Migration()
		};
	}
	
	private static class LunchRestaurantV1Migration implements AstrixJsonMessageMigration<LunchRestaurant> {

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

## Providing the lunch-api with a processing unit

### Service implementations

```java
// The @AstrixServiceExport tells astrix that a given spring bean provides a given set of services.
@AstrixServiceExport({LunchService.class, InternalLunchFeeder.class})
public class LunchServiceImpl implements LunchService, InternalLunchFeeder {
}

// And other service-implementations
```

### Application Descriptor

```java
@AstrixApplication(
	exportsRemoteServicesFor = {
		LunchServiceProvider.class
	},
	component = AstrixServiceComponentNames.GS_REMOTING
)
public class LunchApplicationDescriptor {
}
```

### pu.xml

```xml
<!-- Astrix service framework (provider and consumer) -->
<bean id="astrixFrameworkBean" class="com.avanza.astrix.spring.AstrixFrameworkBean">
	<property name="serviceDescriptor" value="com.avanza.astrix.integration.tests.domain.apiruntime.LunchApplicationDescriptor"/>
	<!-- The subsystem is used by the versionsing framework. Astrix only allows
         invocation of NON-versioned services withing the same subsystem. -->
	<property name="subsystem" value="lunch-system"/>
</bean>

<!-- The actual service implementation(s) -->
<bean id="lunchService" class="com.avanza.astrix.integration.tests.domain.pu.LunchServiceImpl"/>
```



## Consuming the lunch-api

### pu.xml (or an ordinary spring.xml)

```xml
<bean id="astrixFrameworkBean" class="com.avanza.astrix.spring.AstrixFrameworkBean">
	<property name="consumedAstrixBeans">
		<list>
			<value>com.avanza.astrix.integration.tests.domain.api.LunchService</value>
		</list>
	</property>
</bean>
```