# Asterix



# Api Descriptor

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


# pu.xml

    <!-- Asterix service framework (provider and consumer) -->
	<bean id="asterixFrameworkBean" class="se.avanzabank.asterix.context.AsterixFrameworkBean">
		<property name="serviceDescriptor" value="se.avanzabank.asterix.integration.tests.domain.apiruntime.LunchServiceDescriptor"/>
	</bean>
	
	<!-- The actual service implementation(s) -->
    <bean id="lunchService" class="se.avanzabank.asterix.integration.tests.domain.pu.LunchServiceImpl"/>