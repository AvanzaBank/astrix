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

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.core.ServiceUnavailableException;

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
		// The BEAN_BIND_ATTEMPT_INTERVAL determines how often 
		// Astrix will attempt to bind a given bean (millis).
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
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
