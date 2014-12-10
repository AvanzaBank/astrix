/*
 * Copyright 2014-2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tutorial.mocking;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import tutorial.p1.api.LunchRestaurantFinder;
import tutorial.p1.api.LunchSuggester;
import tutorial.p1.provider.LunchSuggesterImpl;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.library.AstrixLibraryProvider;

public class MockingAstrixBeansTest {
	
	@Test
	public void testAstrixConfigurerAllowsRegistrationOfMockInstances() throws Exception {
		LunchRestaurantFinder restaurantFinderStub = Mockito.mock(LunchRestaurantFinder.class);
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		
		// Register the api(s) we intend to test
		astrixConfigurer.registerApiProvider(LunchLibraryProvider.class);
		
		// Stub out its dependency 
		astrixConfigurer.registerAstrixBean(LunchRestaurantFinder.class, restaurantFinderStub);
		AstrixContext astrixContext = astrixConfigurer.configure();
		
		// Get the api we intend to test
		LunchSuggester lunchSuggester = astrixContext.getBean(LunchSuggester.class);
		
		// Stub out getAllRestaurants to allways return one restaurant
		Mockito.stub(restaurantFinderStub.getAllRestaurants()).toReturn(Arrays.asList("Max"));

		assertEquals("Max", lunchSuggester.randomLunchRestaurant());
	}

	@AstrixLibraryProvider
	public static class LunchLibraryProvider {
		
		// Note that this library don't provide the LunchService, lets
		// pretend that its a remote-service provided by another team.
		
		@AstrixExport
		public LunchSuggester lunchUtil(LunchRestaurantFinder restaurantFinder) {
			return new LunchSuggesterImpl(restaurantFinder);
		}
		
	}

}
