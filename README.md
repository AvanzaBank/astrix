# Asterix


## Api 

	public interface LunchService {
		
		@AsterixBroadcast(reducer = LunchSuggestionReducer.class)
		LunchRestaurant suggestRandomLunchRestaurant(String foodType);
		
		void addLunchRestaurant(LunchRestaurant restaurant);
		
		LunchRestaurant getLunchRestaurant(GetLunchRestaurantRequest request); 
	}


## Api Descriptor

	// The API is versioned.
	@AsterixVersioned(
		apiMigrations = {
			LunchApiV1Migration.class
		},	
		version = 2,
		objectMapperConfigurer = LunchApiObjectMapperConfigurer.class
	)
	// The service is exported to the service-registry. Consumers queries the service-registry to bind to servers.
	@AsterixServiceRegistryApi(
		exportedApis = {
			LunchService.class
		}
	)
	public class LunchApiDescriptor {
	}

## Migration

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


## Service implementation

	@AsterixServiceExport({LunchService.class, InternalLunchFeeder.class})
	public class LunchServiceImpl implements LunchService, InternalLunchFeeder {
	}
	

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