# Part 2 - Service Binding
The first part of the tutorial introduced how libraries can be published and consumed using Astrix. This part introduces another type of api, the heart of all service based architectures, namely the service.

In this context, a service is something that is typically provided by a different process. In order to consume a service, Astrix must first bind to the service which is done using an `ServiceComponent`. The `ServiceComponent` interface is the extension point to implement new service-transports in Astrix. As api-developer or consumer you never use the `ServiceComponent` directly. Rather it is used behind the scenes by Astrix to bind to a provider of a given service. However, even if you don't intend to implement you own service-component it's good to have knowledge about the inner workings of service binding.  

Service binding is done in two steps:

1. Lookup the service properties associated with the given service. This involves identifying what `ServiceComponent` to use and find all properties required by the `ServiceComponent` to bind to the service.
2. Use the `ServiceComponent` to bind to the given service.

Each of those two steps offers two levels of indirection. The first one is how to discover the service-properties associated with a service. Out of the box Astrix supports two mechanisms for service discovery. One using configuration, `@AstrixConfigDiscovery`, and another using the service-registry, `@AstrixServiceRegistryDiscovery`, which will be introduced later. The other one is how Astrix binds to a service, which done by the `ServiceComponent`. Astrix comes with a number of `ServiceComponent` implementations out of the box which will be introduced throughout the tutorial. 


### Service binding using DirectComponent and @AstrixConfigDiscovery
The first service-component covered is the `DirectComponent` which is a useful tool to support testing. It allows binding to a service within the same process, i.e. an ordinary object within the same jvm. The true power of the `DirectComponent` comes when using it in combination with the service-registry, which we will se later in the tutorial when introducing the service-registry. This example introduces the `DirectComponent` in combination with a `@AstrixConfigDiscovery`.

In this example the api i split into one library, `LunchSuggester`, and one service, `LunchRestaruantFinder`. A service is provided in a similar way as a library, but by using the `@Service` annotation to define a provided service. Each exported service must also define what discovery-strategy to use to locate the provided services. The method exporting `LunchRestaurantFinder` is annotated with `@AstrixConfigDiscovery` indicating that the properties required to bind to the provided service should be discovered in configuration. 

```java
public interface LunchSuggester {
	String randomLunchRestaurant();
}

@AstrixApiProvider
public class LunchLibraryProvider {
	@Library
	public LunchSuggester lunchSuggester(LunchRestaurantFinder restaurantFinder) {
		return new LunchSuggesterImpl(restaurantFinder);
	}
}
```
 

```java
public interface LunchRestaurantFinder {
	List<String> getAllRestaurants();
}

@AstrixApiProvider
public interface LunchServiceProvider {
	
	@AstrixConfigDiscovery("restaurantFinderUri")
	@Service
	LunchRestaurantFinder lunchRestaurantFinder();

}
```

A unit test for the library api might look like this: 

```java
public class LunchLibraryTest {
	
	private AstrixContext astrix;
	
	@After
	public void after() {
		astrix.destroy();
	}
	
	@Test
	public void astrixDirectComponentAllowsBindingToObjectsInSameProcess() throws Exception {
		LunchRestaurantFinder restaurantFinderStub = Mockito.mock(LunchRestaurantFinder.class);
		String serviceUri = DirectComponent.registerAndGetUri(LunchRestaurantFinder.class, restaurantFinderStub);

		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set("restaurantFinderUri", serviceUri);
		configurer.setBasePackage("tutorial.p2");
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		Mockito.stub(restaurantFinderStub.getAllRestaurants()).toReturn(Arrays.asList("Pontus!"));
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant());
	}

}
```

In the test we want to stub out the `LunchRestaurantFinder` using Mockito. This can easily be done using the `TestAstrixConfigurer` introduced in part 1, but for the sake of illustration we will use the `DirectComponent`. We register the mock with the `DirectComponent` which returns a serviceUri. A serviceUri contains all the service-properties required to bind to a given service. The first part identifies the name of the service component, in this case `"direct"`. The second part is service-component specific and contains all properties required by the service-component to bind to the given service. `DirectComponent` requires an identifier for the target instance which is generated when we register the instance with the `DirectComponent`. Therefore a serviceUri identifying a service provided using the `DirectComponent` might look like this: `"direct:21"`.

When we configure Astrix we provide a setting, `"restaurantFinderUri"` with a value that contains the serviceUri to the `LunchRestaurantFinder` mock instance. When Astrix creates an instance of `LunchRestaurantFinder` (which is done indirectly when we create the `LunchSuggester` bean in the test) the process goes like this:

1. Astrix sees that it is a service (the ApiProvider exports the bean as `@Service`) and that the service properties should be discovered using configuration (defined by the `@AstrixConfigDiscovery` annotation)
2. Astrix queries the configuration for the entry name defined by the `@AstrixConfigDiscovery` annotation ("restaurantFinderUri") to get the serviceUri, lets say that its value is `"direct:21"`
3. Astrix parses the serviceUri to find what `ServiceComponent` to use for binding, in this case `"direct"`
4. Astrix delegates service binding to `DirectComponent`, passing in all component-specific properties, in this case `"21"`
5. The `DirectComponent` queries its internal registry of objects and finds our mock instance and returns it


### Configuration
The previous example uses the configuration mechanism to discover ServiceProperties required to bind to `LunchRestaurantFinder`. Astrixs ships with a small standalone configuration framework called `DynamicConfig`. A configuration property is resolved in the following order:

1. Custom ConfigurationSource's
2. System properties
3. Programmatic configuration set on `AstrixConfigurer`
4. `META-INF/astrix/settings.properties`
5. Default values

Astrix will use the first value found for a given setting. Hence the Custom ConfigurationSource's takes precedence over the Programatic configuration and so on. The custom configuration is plugable by implementing the `ConfigSource` and/or `DynamicConfigSource` spi. By default Astrix will not use any external configuration. The `settings.properties` provides a convenient way to override the default values provided by Astrix. It could be used to set corporate wide default-values by sharing a single `settings.properties` file. For instance it could be used to say that `"com.mycorp"` should be scanned for api-providers, avoiding the need to dupplicate such configurations on every instance of AstrixConfigurer throughout an enterprise.

```java
TODO: configuration example?
```


### Stateful Astrix Beans
Every service-bean in astrix (any bean bound using an `ServiceComponent`) will be a "stateful" bean. `Astrix.getBean(BeanType.class)` always returns a proxy for a stateful bean (provided that there exists an `AstrixServiceProvider` exporting the given api). However, if the bean can't be bound the proxy will be in `UNBOUND` state in which it will throw a `ServiceUnavailableException` on each invocation. Astrix will periodically attempt to bind an `UNBOUND` bean until successful.

The following example illustrates how a service-bean proxy goes from `UNBOUND` state to `BOUND` when the target service becomes available. It also illustrates usage of AstrixSettings as an external configuration provider which can be useful in testing.

```java

public class AstrixBeanStateManagementTest {
	
	private AstrixSettings settings = new AstrixSettings();
	private AstrixContext astrix;
	
	@After
	public void after() {
		astrix.destroy();
	}
	
	@Test
	public void astrixManagesStateForEachServiceBean() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10); // 1.
		configurer.set(AstrixSettings.ASTRIX_CONFIG_URI, settings.getExternalConfigUri()); // 2.
		configurer.setBasePackage("tutorial.p2");
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		try {
			lunchSuggester.randomLunchRestaurant(); // 3.
		} catch (ServiceUnavailableException e) {
			// LunchRestaurantFinder is UNBOUND
		}
		
		LunchRestaurantFinder restaurantFinder = Mockito.mock(LunchRestaurantFinder.class);

		String serviceUri = DirectComponent.registerAndGetUri(LunchRestaurantFinder.class, restaurantFinder); // 4.
		settings.set("restaurantFinderUri", serviceUri); // 5.
		
		astrix.waitForBean(LunchSuggester.class, 2000); // 6.
		
		Mockito.stub(restaurantFinder.getAllRestaurants()).toReturn(Arrays.asList("Pontus!"));
		
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant()); // 7.
	}
}

```

1. The `BEAN_BIND_ATTEMPT_INTERVAL` determines how often Astrix will attempt to bind a given bean (millis).
2. The `ASTRIX_CONFIG_URI` is a service-like uri to the external configuration source, in this case an instance of `AstrixSettings` within the same process.
3. The `LunchSuggester` uses `LunchRestaurantFinder` in background. Since the configuration contains no entry for  `"restarurantFinderUri"` the `LunchRestaurantFinder` will be in state `UNBOUND`
4. This registers a mock instance for `LunchRestaurantFinder` in the direct-component, which returns a serviceUri that can be used to bind to the mock instance.
5. A `"restaurantFinderUri"` pointing to the mock is added to the configuration.
6. Astrix allows us to wait for a bean to be bound. Note that we are waiting for a Library. Astrix is clever and detects that the library uses the `LunchRest
7. aurantFinder` and therefore waits until the `LunchRestaurantFinder` is bound.
7. The `LunchSuggester` library is invoked, which in turn invokes the `LunchRestaurantFinder` service which will be `BOUND` at this time.


[Next: Part 3 - The Service Registry](part3.md)  