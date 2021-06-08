/*
 * Copyright 2014 Avanza Bank AB
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

import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tutorial.p2.api.LunchRestaurantFinder;
import tutorial.p2.api.LunchSuggester;
import tutorial.p2.provider.LunchLibraryProvider;
import tutorial.p2.provider.LunchServiceProvider;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LunchLibraryTest {
	
	private AstrixContext astrix;
	
	@AfterEach
	void after() {
		astrix.destroy();
	}
	
	@Test
	void astrixDirectComponentAllowsBindingToObjectsInSameProcess() {
		LunchRestaurantFinder restaurantFinderStub = mock(LunchRestaurantFinder.class);
		String serviceUri = DirectComponent.registerAndGetUri(LunchRestaurantFinder.class, restaurantFinderStub);

		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set("restaurantFinderUri", serviceUri);
		configurer.registerApiProvider(LunchServiceProvider.class);
		configurer.registerApiProvider(LunchLibraryProvider.class);
		astrix = configurer.configure();
		
		LunchSuggester lunchSuggester = astrix.getBean(LunchSuggester.class);

		when(restaurantFinderStub.getAllRestaurants()).thenReturn(singletonList("Pontus!"));
		assertEquals("Pontus!", lunchSuggester.randomLunchRestaurant());
	}

}
