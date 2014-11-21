# Part 1 - The IOC container
One of the core features of Astrix is to work as a factory for mircoservices. In that regard, Astrix is similar to other IOC frameworks like Spring and Guice, although Astrix is not intended as the main application IOC-contiainer. Rather Astrix complements another IOC-container, for instance spring which Astrix is well integrated with, to create services, whereas the other IOC-framwork is responsible for wiring-together all application-beans and managing their lifecycle.

At runtime, every object that Astrix creates is called an astrix-bean, which is similar to a bean in spring.

On type of api in astrix is called `Library`. A Library consists of a number of public interfaces/classes and associated implementations. Astrix shields a library provider from the consumers of the libraries by allowing the consumer to "program against interfaces" without ever needing to now what implements the given interfaces, or how the classes that implement the interfaces are assembled.

### Example Library 

The first step in creating a library is to define the public api. This example only contians a simple LunchUtil interface:

```java
public interface LunchUtil {
	String randomLunchRestaurant();
}
```


The second step is to implement the library. Its a good practice to put the public api and the implementation in different modules, since a consumer of the api have compile-time dependency to the public api, but only a runtime dependency to the implementation.

```java
public class LunchUtilImpl implements LunchUtil {
	
	private Random rnd = new Random();
	
	@Override
	public String randomLunchRestaurant() {
		List<String> restaurants = getAllRestaurants();
		return restaurants.get(rnd.nextInt(restaurants.size()));
	}

	private List<String> getAllRestaurants() {
		return AllLunchRestaurants.ALL_RESTAURANTS;
	}

}
```

Finally, in order to use Astrix as a factory to create an instance of the given library one must create a library-descriptor, which is typically located in the same module as the implementing classes:

```java
@AstrixLibraryProvider
public class LunchLibraryProvider {
	
	@AstrixExport
	public LunchUtil lunchUtil() {
		return new LunchUtilImpl();
	}

}
```
 
The `@AstrixLibraryProvider` tells Astrix that this class can build library apis. Each method annotated with `@AstrixExport` will act as a factory method to create the given api element, which is defined by the return type of thte factory method, which is `LunchUtil` in this example.



Consumption of LunchApi is done by first creating an `AstrixContext` and then use it as a factory. This unit-test exemplifies:


```java
public class LunchLibraryTest {
	
	private AstrixContext astrix;
	
	@After
	public void after() {
		astrix.destroy();
	}

	@Test
	public void lunchUtilCanBeConsumedUsingAstrix() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setBasePackage("tutorial.t1");
		astrix = configurer.configure();
		
		LunchUtil bean = astrix.getBean(LunchUtil.class);
		
		String restaurant = bean.randomLunchRestaurant();
		assertTrue(AllLunchRestaurants.ALL_RESTAURANTS.contains(restaurant));
	}
	

}
```

An AstrixContext is created using an AstrixConfigurer. By default, astrix won't scan the classpath for api-providers. The `AstrixConfigurer.setBasePackage` tells Astrix to scan the "tutorial.t1" package, and all its subpackages for api-providers.