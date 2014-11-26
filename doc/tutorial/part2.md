# Part 2 - Service Binding
The first part of the tutorial introduced how librarires can be created and consumed with Astrix. In this part we will introduce another type of api, the heart in all service based architectures, namely the service.

In this context, a service is something that is typically provided by a different process. Therefore, in order to consume a service, Astrix must first bind to the given service, which is done in two steps:

1. Locate a provider of the given service. This involves identifying what `AstrixServiceComponent` to use to bind to the service and find all properties required by the `AstrixServiceComponent` to bind to the service.
2. Use the `AstrixServiceComponent` to bind to the given service.

There are two deegres of freedom involved here. The first one is how a service is located. Out of the box Astrix supports two mechanisms for locating a service. One using configuration, `@AstrixConfigApi`, and another using the service-registry, `@AstrixServiceRegistryApi`, which will be introduced later. The second degree of freedom is how Astrix binds to a service, which done by the `AstrixServiceComponent`. Astrix comes with a number of service-component implementations out of the box which will be introduced throughout the tutorial. 

Astrix has an extendable service-binding mechanism through the `AstrixServiceComponent` interface. As an api-developer or consumer, you never use the `AstrixServiceComponent` directly. Rather its used behind the sceenes by Astrix to bind to a provider of a given service. However, even if you don't intend to implement you own service-component it's good to have knowledge about the inner workings of service binding.

### Service binding using AstrixDirectComponent an @AstrixConfigApi
The first service-component we will cover is the `AstrixDirectComponent` which is a useful tool to support testing. It allows binding to a service provider within the same process, ie an ordinary object within the same jvm. The next example introduces the `AstrixDirectComponent` and `@AstrixConfigApi`.

In the example the api i split into one library, `LunchSuggester`, and one service, `LunchRestaruantFinder`. The api-descriptor exporting the `LunchRestaurantFinder` tells Astrix that the service should be located using configuration, and that the configuration entry is "restaurantFinderUri".

```java
public interface LunchSuggester {
	String randomLunchRestaurant();
}

@AstrixLibraryProvider
public class LunchLibraryProvider {
	@AstrixExport
	public LunchSuggester lunchSuggester(LunchRestaurantFinder restaurantFinder) {
		return new LunchSuggesterImpl(restaurantFinder);
	}
}
```
 

```java
public interface LunchRestaurantFinder {
	List<String> getAllRestaurants();
}

@AstrixConfigApi(
	exportedApis = LunchRestaurantFinder.class,
	entryName = "restaurantFinderUri"
)
public class LunchServiceProvider {
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
	public void astrixDirectComponentAllowsBindingToObjectsInTheSameProcess() throws Exception {
		LunchRestaurantFinder restaurantFinder = Mockito.mock(LunchRestaurantFinder.class);
		String serviceUri = AstrixDirectComponent.registerAndGetUri(LunchRestaurantFinder.class, restaurantFinder);

		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set("restaurantFinderUri", serviceUri);
		configurer.setBasePackage("tutorial.p2");
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		Mockito.stub(restaurantFinder.getAllRestaurants()).toReturn(Arrays.asList("Pontus!"));
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant());
	}

}
```

In the test we want to stub out the `LunchRestaurantFinder` using Mockito. This could easily be done using the `TestAstrixConfigurer` introduced in part 1, but for the sake of illustration we will use the `AstrixDirectComponent`. We register the mock with the `AstrixDirectComponent` which returns a serviceUri. A serviceUri consists of two parts. The first part identifies the name of the service component, in this case `"direct"`. The second part is service-component specific and contains all properties required by the service-component to bind to the given service. When using the `AstrixDirectComponent` it contains an identifier for the instance which is generated when we register the instance with the `AstrixDirectComponent`. Therefore a serviceUri identifying a service provided using the `AstrixDirectComponent` might look like this: `"direct:21"`.

When we configure Astrix we provide a setting, "restaurantFinderUri" with a value that contains the serviceUri to the `LunchRestaurantFinder` mock instance. When Astrix is requested to create an instance of `LunchRestaurantFinder` (which is done indirectly when we create the `LunchSuggester` bean in the test) the process goes like this:

1. Astrix sees that its a configuration-api (`@AstrixConfigApi`)
2. Astrix queries the configuration for the entry name defined by the annotation ("restaurantFinderUri") to get the serviceUri, lets say that its value is `"direct:23"`
3. Astrix parses the serviceUri to find what `AstrixServiceComponent` to use for binding, in this case `"direct"`
4. Astrix delegates service binding to `AstrixDirectComponent`, passing in all component-specific properties, in this case `"21"`
5. The `AstrixDirectComponent` queries its internal registry of objects and finds our mock instance and returns it


### Configuration
In the example above we use the configuration mechanism to locate the provider of `LunchRestaurantFinder`. The configuration mechanism in Astrix is hierarchical and an entry is resolved in the following order.

1. External configuration
2. Programatic configuration set on AstrixConfigurer
3. META-INF/astrix/settings.properties
4. Default values

Astrix will used the first value found for a given setting. Hence the External Configuration takes precedence over the Programatic configuration and so on. The external configuration is pluggable by implementing ´AstrixExternalConfig´. By default Astrix will not use any external configuration.

TODO: configuration example?


### Stateful Astrix Beans
Every bean that is bound using an `AstrixServiceComponent` will be a "stateful" bean. `Astrix.getBean(BeanType.class)` always returns a proxy for a given bean (provided that there exists an api-descriptor exporting the given api). However, if the bean can't be bound the proxy will be in UNBOUND state in which it will throw a `ServiceUnavailableException` upon each invocation. Astrix will periodically attempt to bind the bean until successful.


The following example illustrates how a bean goes from UNBOUND state to BOUND when a given service becomes available. It also illustrates usage of AsterixSettings as an external configuration provider which can be usefull in testing.

```java

public class AstrixBeanStateManagementTest {
	
	private AstrixSettings settings = new AstrixSettings();
	private AstrixContext astrix;
	
	@After
	public void after() {
		astrix.destroy();
	}
	
	@Test
	public void astrixManagesStateForEachServiceBean2() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		// The BEAN_REBIND_ATTEMPT_INTERVAL determines how often 
		// Astrix will attempt to bind a given bean (millis).
		configurer.set(AstrixSettings.BEAN_REBIND_ATTEMPT_INTERVAL, 10);
		// Set the uri to the external config.
		configurer.set(AstrixSettings.ASTRIX_CONFIG_URI, settings.getExternalConfigUri());
		configurer.setBasePackage("tutorial.p2");
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		try {
			// Since the LunchSuggester uses LunchRestaurantFinder in background
			// but currently configuration doesn not contain a 'restarurantFinderUri'
			// so it will be in state UNBOUND
			lunchSuggester.randomLunchRestaurant();
		} catch (ServiceUnavailableException e) {
			// No service available
		}
		
		
		LunchRestaurantFinder restaurantFinder = Mockito.mock(LunchRestaurantFinder.class);

		// Register mock instance in direct-component
		String serviceUri = AstrixDirectComponent.registerAndGetUri(LunchRestaurantFinder.class, restaurantFinder);
		// Add restaurantFinderUri entry to configuration pointing to the mock
		settings.set("restaurantFinderUri", serviceUri);
		
		// Astrix allows us to wait for a bean to be bound
		// Note that we are waiting for a Library. Astrix is clever and
		// Detects that the library uses the LunchRestaurantFinder and therefore
		// waits until the LunchRestaurantFinder is bound
		astrix.waitForBean(LunchSuggester.class, 2000);
		
		Mockito.stub(restaurantFinder.getAllRestaurants()).toReturn(Arrays.asList("Pontus!"));
		
		// Invoke library which will in turn invoke the mock.
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant());
	}
}

```





### Service Binding
- GsComponent

* Illustrate service-binding with AstrixConfigApi and DirectComponent (DONE)
* Illustrate that a service must not be available when bean is created
* Illustrate waiting for library bean to be bound


### Service Registry (Service discovery)
* Dynamic service discovery
* Relies on service-binding using AstrixServiceComponent


### Astrix configuration
* Extension points
* Default hierarchy