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
package lunch.tests;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import com.avanza.gs.test.junit5.PuConfigurers;
import com.avanza.gs.test.junit5.RunningPu;
import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LunchIntegrationTest {
	
	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@RegisterExtension
	static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											.contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
											.configure();
	@RegisterExtension
	AutoCloseableExtension autoClosables = new AutoCloseableExtension();
	
	@Test
	void testName() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 500);
		configurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixContext astrix = autoClosables.add(configurer.configure());
		LunchService lunchService = astrix.waitForBean(LunchService.class, 15_000);
		
		lunchService.addLunchRestaurant(new LunchRestaurant("McDonalds", "Fast Food"));
		
		assertNotNull(lunchService.getLunchRestaurant("McDonalds"));
	}

}
