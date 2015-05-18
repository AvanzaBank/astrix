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
package tutorial.p2;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import tutorial.p2.api.LunchRestaurantFinder;
import tutorial.p2.api.LunchSuggester;
import tutorial.p2.provider.LunchLibraryProvider;
import tutorial.p2.provider.LunchServiceProvider;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.DirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;

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

		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set("restaurantFinderUri", serviceUri);
		configurer.registerApiProvider(LunchServiceProvider.class);
		configurer.registerApiProvider(LunchLibraryProvider.class);
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		Mockito.stub(restaurantFinderStub.getAllRestaurants()).toReturn(Arrays.asList("Pontus!"));
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant());
	}

}
