# Creating a Library
Libraries are used to execute domain logic on the consumer side. Libraries are allowed to depend on other astrix-managed beans (remote-services, other libraries, etc)

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
