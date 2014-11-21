# Part 1 - The IOC container
One of the core features of Astrix is to work as a factory for mircoservices. In that regard, Astrix is similar to other IOC frameworks like Spring or Guice. However, Astrix is not intended to be used as a standalone IOC-container. Rather Astrix complements another IOC-container acting as a factory for binding to (micro)services, whereas the other IOC-framwork is responsible for wiring together all application-beans and managing their lifecycle.

#### Why two IOC-containers?
A typicall IOC container like spring is well suited to provide loose coupling between the objects that builds up the application. A good practice when developing spring applications is to "program against interface's" wich means that the different application objects only know each other by interface. This works well for fairly large applications. 

However, a problem arise when building a system that consists of hundereds of microservices, typcially developed by different teams/individualls. Sharing libraries/clients in such an organization can lead to difficulties in maintainance if care is not taken to clearly define what is part of the public-api, and what should constitue an internal implementation detail. In such a case its not only important that the different application objects know each other through an interface, but also that different applications don't know how the libraries/clients provided by other teams are assembled.

Wheras a normal IOC-container inverses the responsibility for application assembly and lifecycle management, Astrix inverses the responsibility for api assembly.

### ApplicationContext and AstrixContext  
![AstrixContext](AstrixIOC.png)

At runtime, every object that is part of an api that Astrix creates is called an astrix-bean, which is similar to a bean in spring. In order for Astrix to be able to create an astrix-bean of a given type, an `ApiProvider` for the given api must exist. Astrix has an extendable `ApiProvider` mechanism, which allows new api "types" to be plugged into Astrix. Two common api types that are supported out of the box are `Library` and `ServiceRegistryApi`.

A `Library` consist of a number of public interfaces/classes and associated implementations. Astrix shields a library provider from the consumers of the library by allowing the consumer to "program against interfaces" without ever needing to now what implements the given interfaces, or how the classes that implement the interfaces are assembled.

### A Simple Library 
The code for this part can be found [here](../../tree/master/tutorial/src/main/java/tutorial/t1). 

The first step in creating a library is to define the public api. This example only contains a LunchUtil interface:

```java
public interface LunchUtil {
	String randomLunchRestaurant();
}
```


The second step is to implement the library. Its a good practice to put the public api and corresponding implementation in different modules, since a consumer of the api have compile-time dependency to the public api, but only a runtime dependency to the implementation.

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

Finally, in order to use Astrix as a factory to create instances of the given library one must create a library-descriptor, which is typically located in the same module as the implementing classes:

```java
@AstrixLibraryProvider
public class LunchLibraryProvider {
	
	@AstrixExport
	public LunchUtil lunchUtil() {
		return new LunchUtilImpl();
	}

}
```
 
The `@AstrixLibraryProvider` annotation tells Astrix that this class if a factory for a library api. Each method annotated with `@AstrixExport` will act as a factory method to create a given api element, which is defined by the return type of the factory method, which is `LunchUtil` in this example.

### Consuming the Library
Consumption of the LunchApi is done by first creating an `AstrixContext` and then use it as a factory. This unit-test exemplifies:


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

An AstrixContext is created using an AstrixConfigurer. By default, astrix won't scan the classpath for api-providers. The `AstrixConfigurer.setBasePackage` tells Astrix to scan the "tutorial.t1" package, and all its subpackages for api-providers. In this case Astrix will find that the `LunchLibraryProvider` provides the `LunchUtil` api-element, and use it as a factory to create instances of `LunchUtil`.