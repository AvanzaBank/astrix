# Asterix

Asterix is a service framework used to create an application runtime for an application that consumes other services. It's the like a dependency-injection framework on a service-level.


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