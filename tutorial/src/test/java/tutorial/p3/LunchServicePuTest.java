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
package tutorial.p3;

import static org.junit.Assert.*;

import javax.annotation.PostConstruct;

import org.junit.ClassRule;
import org.junit.Test;

import tutorial.p3.api.LunchService;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.service.registry.util.InMemoryServiceRegistry;

public class LunchServicePuTest {
	
	public static InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/p3/lunch-pu.xml")
												   .contextProperty("externalConfigUri", serviceRegistry.getExternalConfigUri())
												   .configure();

	private AstrixContext astrix;
	
	@PostConstruct
	public void destroy() {
		astrix.destroy();
	}
	
	
	@Test
	public void testName() throws Exception {
		AstrixConfigurer astrixConfigurer = new AstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.ASTRIX_CONFIG_URI, serviceRegistry.getExternalConfigUri());
		astrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		astrixConfigurer.setBasePackage("tutorial.p3");
		astrix = astrixConfigurer.configure();
		
		LunchService lunchService = astrix.waitForBean(LunchService.class, 10_000);
		
		lunchService.addLunchRestaurant("FEI");
		
		assertEquals("FEI", lunchService.getAllRestaurants().get(0));
	}
	
	

}
