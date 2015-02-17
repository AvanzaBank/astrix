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

import static org.junit.Assert.assertNotNull;
import lunch.api.LunchRestaurant;
import lunch.api.LunchService;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.service.registry.util.InMemoryServiceRegistry;

public class LunchIntegrationTest {
	
	public static InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
												   .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
												   .configure();
	static {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("com.avanza").setLevel(Level.DEBUG);
	}
	
	@Test
	public void testName() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 500);
		configurer.set(AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixContext astrix = configurer.configure();
		LunchService lunchService = astrix.waitForBean(LunchService.class, 15_000);
		
		lunchService.addLunchRestaurant(new LunchRestaurant("McDonalds", "Fast Food"));
		
		assertNotNull(lunchService.getLunchRestaurant("McDonalds"));
	}

}
