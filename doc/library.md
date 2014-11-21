# Creating a Library
Libraries are used to execute domain logic on the consumer side. Libraries are allowed to depend on other astrix-managed beans (remote-services, other libraries, etc)

## Providing a Library

### Example API
```java
public interface LunchUtil {
	LunchRestaurant suggestRandomRestaurant();
}
``` 

### Example API Provider
```java
public class LunchUtilImpl implements LunchUtil {

	private LunchService lunchService;

	public LunchUtilImpl(LunchService lunchService) {
		this.lunchService = lunchService;
	}

	@Override
	public LunchRestaurant suggestRandomRestaurant() {
		List<LunchRestaurant> allRestaurants = lunchService.getAllLunchRestaurants();
		return randomElement(allRestaurants);
	}
}
``` 

```java
@AstrixLibraryProvider
public class LunchApiFactory {

	@AstrixExport
	public LunchUtil createLunchUtil(LunchService lunchService) {
		return new LunchUtilImpl(lunchService);
	}
	
}
``` 

## Consuming a Library

### Consumer - Alt 1 Standalone
```java
public void consumerMethod() {
		AstrixConfigurer configurer = new AstrixConfigurer();
		AstrixContext astrix = configurer.configure();
		LunchUtil lunchUtil = astrix.getBean(LunchUtil.class);
}
``` 

### Consumer - Alt 2 Spring-application (xml config)
```xml
<bean id="astrixFrameworkBean" class="com.avanza.astrix.context.AstrixFrameworkBean">
	<property name="consumedAstrixBeans">
		<list>
			<value>lunch.api.LunchUtil</value>
		</list>
	</property>
</bean>

<!-- Other beans: LunchUtil will be available as an autowire candiate -->
``` 

### Consumer - Alt 3 Spring-application (java config)
```xml
@Configuration
public class AppConfig {
	
	@Bean
	public AstrixFrameworkBean astrix() {
		AstrixFrameworkBean result = new AstrixFrameworkBean();
		result.setSubsystem("lunch-service");
		result.setConsumedAstrixBeans(LunchUtil.class);
		return result;
	}
	
	@Bean
	public MyAppClass createAppClass(LunchUtil lunchUtil) {
		return new MyAppClass(lunchUtil);
	}
}
``` 


