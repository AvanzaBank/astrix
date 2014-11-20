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
package lunch.tests;

import static org.junit.Assert.*;
import lunch.api.LunchRestaurant;
import lunch.api.LunchService;

import org.junit.ClassRule;
import org.junit.Test;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.service.registry.util.InMemoryServiceRegistry;

public class LunchIntegrationTest {
	
	public static InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
												   .serviceRegistryUri(serviceRegistry.getServiceUri())
												   .configure();
	
	public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
			   											 .serviceRegistryUri(serviceRegistry.getServiceUri())
														 .configure();
	
	@Test
	public void testName() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set(AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixContext astrix = configurer.configure();
		LunchService lunchService = astrix.waitForBean(LunchService.class, 5000);
		
		lunchService.addLunchRestaurant(new LunchRestaurant("McDonalds", "Fast Food"));
		
		assertNotNull(lunchService.getLunchRestaurant("McDonalds"));
	}

}
