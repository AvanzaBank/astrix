# Publish a service to the Service Registry

### API
```java
public interface LunchService {
	void addLunchRestaurant(LunchRestaurant restaurant);
	LunchRestaurant getLunchRestaurant(@Routing String name); 
}
```

### API Provider
```java
public interface LunchService {
	void addLunchRestaurant(LunchRestaurant restaurant);
	LunchRestaurant getLunchRestaurant(@Routing String name); 
}

@AstrixServiceRegistryApi(
	LunchService.class
)
public class LunchApiDescriptor {
}
```



### Server
```java
@AstrixService(
	apiDescriptors = {
		LunchApiDescriptor.class,
	},
	component = AstrixServiceComponentNames.GS_REMOTING
)
public class LunchServiceDescriptor {
}
```


### ALT 1 - XML based configuration
```xml
<bean id="astrixFrameworkBean" class="com.avanza.astrix.context.AstrixFrameworkBean">
	<property name="serviceDescriptor" value="lunch.pu.LunchServiceDescriptor"/>
	<property name="subsystem" value="lunch-service"/>
</bean>
    
<!-- The actual service implementation(s) -->
<bean id="lunchService" class="lunch.pu.LunchServiceImpl"/>
```

### ALT 2 - Java based configuration

```java
public class LunchConfig {
	
	@Bean
	public AstrixFrameworkBean astrix() {
		AstrixFrameworkBean result = new AstrixFrameworkBean();
		result.setSubsystem("lunch-service");
		result.setServiceDescriptor(LunchServiceDescriptor.class);
		return result;
	}
	
	@Bean
	public LunchService lunchService() {
		// 
	}
}
```