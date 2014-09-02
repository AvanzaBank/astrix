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
package se.avanzabank.asterix.integration.tests;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static se.avanzabank.asterix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openspaces.core.GigaSpace;

import se.avanzabank.asterix.context.Asterix;
import se.avanzabank.asterix.context.AsterixConfigurer;
import se.avanzabank.asterix.integration.tests.domain.api.GetLunchRestaurantRequest;
import se.avanzabank.asterix.integration.tests.domain.api.LunchRestaurant;
import se.avanzabank.asterix.integration.tests.domain.api.LunchService;
import se.avanzabank.asterix.integration.tests.domain.api.LunchUtil;
import se.avanzabank.asterix.integration.tests.domain2.api.LunchRestaurantGrader;
import se.avanzabank.asterix.remoting.client.AsterixRemoteServiceException;
import se.avanzabank.core.test.util.jndi.EmbeddedJndiServer;
import se.avanzabank.core.test.util.jndi.JndiServerRule;
import se.avanzabank.core.test.util.jndi.JndiServerRuleHook;
import se.avanzabank.space.UsesLookupGroupsSpaceLocator;
import se.avanzabank.space.junit.pu.PuConfigurers;
import se.avanzabank.space.junit.pu.RunningPu;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixIntegrationTest {
	
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											  .numberOfPrimaries(1)
											  .numberOfBackups(0)
											  .configure();
	
	public static RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
													  .numberOfPrimaries(1)
													  .numberOfBackups(0)
													  .configure();
	
	public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
																  .numberOfPrimaries(1)
																  .numberOfBackups(0)
																  .configure();
	
	public static JndiServerRule jndi = new JndiServerRule(new JndiServerRuleHook() {
		@Override
		public void afterStart(EmbeddedJndiServer jndiServer) {
			jndiServer.addValueEntry("service-registry-space", "jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
		}
	});
	
	@ClassRule
	public static RuleChain order = RuleChain.outerRule(serviceRegistrypu).around(jndi).around(lunchPu).around(lunchGraderPu);

	
	
	private LunchService lunchService;
	private LunchUtil lunchUtil;
	private LunchRestaurantGrader lunchRestaurantGrader;
	
	static {
		// TODO: remove debugging information
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Logger.getLogger("se.avanzabank.service.suite").setLevel(Level.DEBUG);
	}
	
	@Before
	public void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		AsterixConfigurer configurer = new AsterixConfigurer();
		configurer.registerDependency(new UsesLookupGroupsSpaceLocator(serviceRegistrypu.getLookupGroupName())); // For service-registry-discovery
		configurer.useFaultTolerance(false);
		configurer.enableVersioning(true);
		Asterix asterix = configurer.configure();
		Thread.sleep(1000); // TODO: wait for service to be registered in service registry in clean way...
//		this.lunchService = asterix.waitForService(LunchService.class, 5000);
//		this.lunchUtil = asterix.waitForService(LunchUtil.class, 5000);
		this.lunchService = asterix.getBean(LunchService.class);
		this.lunchUtil = asterix.getBean(LunchUtil.class);
		this.lunchRestaurantGrader = asterix.getBean(LunchRestaurantGrader.class);

	}
	
	@Test
	public void testPuThatConsumesOtherServcies() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		lunchRestaurantGrader.grade("Martins Green Room", 2);
		lunchRestaurantGrader.grade("Martins Green Room", 4);
		
		assertEquals(3.0, lunchRestaurantGrader.getAvarageGrade("Martins Green Room"), 0.01D);
	}

	
	@Test
	public void routedRequestDemo() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		LunchRestaurant r = lunchService.getLunchRestaurant(request);
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void broadcastedRequest_InteractingWithServiceActivator() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		LunchRestaurant r = lunchService.suggestRandomLunchRestaurant("vegetarian");
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void routedRequest_throwsException() throws Exception {
		try {
			GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
			request.setName("throwException"); // LunchServiceImpl is hard-coded to throw exception for this name.
			lunchService.getLunchRestaurant(request);
		} catch (AsterixRemoteServiceException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("[java.lang.IllegalArgumentException: Illegal restaurant: throwException]"));
		}
	}
	
	@Test
	public void useLibrary() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").withFoodType("vegetarian").build());
		LunchRestaurant r = lunchUtil.suggestVegetarianRestaurant();
		assertEquals("Martins Green Room", r.getName());
	}
}
