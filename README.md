# Asterix

Asterix is a framework designed to simplify development and maintenance of microservices. It is used by service providers to publish its provided services, and by service comsumers to bind to published services. Most applications do both, they provide services as well as comsume other services.

Some of the features provided:
- service publishing/discovery
- service binding
- service versioning
- fault tolerance

It's designed to support an organization where many teams develop different services and make those services available for other teams using Asterix.

## Service Registry
A core component in the framework is the service registry. It’s an application that allows service-providers to register all services hey provide. The service-registry is also used by service-consumers to discover providers of a cobsumed service.


## Service Binding
One of the main responsibilities for Asterix is service binding. It requires Asterix to somehow locate a provider for a given service, and then bind the current client to that service. Typically this involves using the service-registry to dynamicallt discover service providers, and then use the information retrieved to bind directly to the service-provider. It's also possible to locate providers without using the service-registry, for instance using configuration.

If the service is located using the service-registry, then a lease-manager thread will run for the given service in the background. The lease-manager will periodically ask the service-registry for information about where the given service is located, and if the service has moved the lease-manager will rebind the to the new provider.

## Spring Integration
Asterix is well integrated with spring.


Typical module-layout when creating a service using Asterix: 
* API
* API provider
* Server

## Example - The LunchApi

Modules
* lunch-api
* lunch-api-provider
* lunch-server 


## API (lunch-api) 

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
	

## API descriptor (lunch-api-provider)

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

## Migration (lunch-api-provider)

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


## Service implementations

	@AsterixServiceExport({LunchService.class, InternalLunchFeeder.class})
	public class LunchServiceImpl implements LunchService, InternalLunchFeeder {
	}
	
	// And other service-implementations
	

## Service Descriptor

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

## pu.xml

    <!-- Asterix service framework (provider and consumer) -->
	<bean id="asterixFrameworkBean" class="se.avanzabank.asterix.context.AsterixFrameworkBean">
		<property name="serviceDescriptor" value="se.avanzabank.asterix.integration.tests.domain.apiruntime.LunchServiceDescriptor"/>
	</bean>
	
	<!-- The actual service implementation(s) -->
    <bean id="lunchService" class="se.avanzabank.asterix.integration.tests.domain.pu.LunchServiceImpl"/>