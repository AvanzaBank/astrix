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
package com.avanza.astrix.integration.tests;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContextImpl;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.IllegalSubsystemException;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchServiceAsync;
import com.avanza.astrix.integration.tests.domain.api.LunchStatistics;
import com.avanza.astrix.integration.tests.domain.api.LunchUtil;
import com.avanza.astrix.integration.tests.domain.apiruntime.feeder.InternalLunchFeeder;
import com.avanza.astrix.integration.tests.domain2.api.LunchRestaurantGrader;
import com.avanza.astrix.integration.tests.domain2.apiruntime.PublicLunchFeeder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.remoting.client.AstrixRemoteServiceException;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistry;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryClient;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.test.util.Supplier;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixIntegrationTest {
	
	
	
	@ClassRule
	public static RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
															.numberOfPrimaries(1)
															.numberOfBackups(0)
															.beanProperties("space", new Properties() {{
																// Run lease-manager thread every 200 ms.
																setProperty("space-config.lease_manager.expiration_time_interval", "200");
															}})
															.startAsync(true)
															.configure();
	
	private static AstrixSettings config = new AstrixSettings() {{
		set(ASTRIX_SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
		set(SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
	}};
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											  .numberOfPrimaries(1)
											  .numberOfBackups(0)
											  .contextProperty("configUrl", config.getExternalConfigUrl())
											  .startAsync(true)
											  .configure();
	
	@ClassRule
	public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
														.numberOfPrimaries(1)
														.numberOfBackups(0)
														.contextProperty("configUrl", config.getExternalConfigUrl())
														.startAsync(true)
														.configure();

	private LunchService lunchService;
	private LunchUtil lunchUtil;
	private LunchRestaurantGrader lunchRestaurantGrader;
	private LunchServiceAsync asyncLunchService;
	private PublicLunchFeeder publicLunchFeeder;
	private AstrixContextImpl astrix;
	private AstrixServiceRegistryClient serviceRegistryClient;

	static {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Logger.getLogger("com.avanza.astrix").setLevel(Level.DEBUG);
	}
	
	@Before
	public void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.enableFaultTolerance(true);
		configurer.enableVersioning(true);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		configurer.set(AstrixSettings.ASTRIX_CONFIG_URI, config.getExternalConfigUrl());
		configurer.setSubsystem("test-sub-system");
		astrix = configurer.configure();
		this.lunchService = astrix.getBean(LunchService.class);
		this.lunchUtil = astrix.getBean(LunchUtil.class);
		this.lunchRestaurantGrader = astrix.getBean(LunchRestaurantGrader.class);
		this.asyncLunchService = astrix.getBean(LunchServiceAsync.class);
		this.publicLunchFeeder = astrix.getBean(PublicLunchFeeder.class);
		this.serviceRegistryClient = astrix.getBean(AstrixServiceRegistryClient.class);
		astrix.waitForBean(LunchService.class, 5000);
		astrix.waitForBean(LunchUtil.class, 5000);
		astrix.waitForBean(LunchRestaurantGrader.class, 5000);
		astrix.waitForBean(LunchServiceAsync.class, 5000);
		astrix.waitForBean(PublicLunchFeeder.class, 5000);
		astrix.waitForBean(AstrixServiceRegistry.class, 5000);
		astrix.waitForBean(LunchStatistics.class, 5000);
	}
	
	@Test
	public void testPuThatConsumesAnotherService() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		lunchRestaurantGrader.grade("Martins Green Room", 2);
		lunchRestaurantGrader.grade("Martins Green Room", 4);
		
		assertEquals(3.0, lunchRestaurantGrader.getAvarageGrade("Martins Green Room"), 0.01D);
	}

	@Test
	public void routedRemotingRequest() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		LunchRestaurant r = lunchService.getLunchRestaurant(request);
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void broadcastedRemotingRequest() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		LunchRestaurant r = lunchService.suggestRandomLunchRestaurant("vegetarian");
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void routedRemotingRequest_throwsException() throws Exception {
		try {
			GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
			request.setName("throwException"); // LunchServiceImpl is hard-coded to throw exception for this name.
			lunchService.getLunchRestaurant(request);
		} catch (AstrixRemoteServiceException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Illegal restaurant: throwException]"));
		}
	}
	
	@Test
	public void libraryUsageTest() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").withFoodType("vegetarian").build());
		LunchRestaurant r = lunchUtil.suggestVegetarianRestaurant();
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void asyncService() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		Future<LunchRestaurant> f = asyncLunchService.getLunchRestaurant(request);
		LunchRestaurant r = f.get(300, TimeUnit.MILLISECONDS);
		assertEquals("Martins Green Room", r.getName());
	}
	
	@Test
	public void itsOkToInvokeUnversionedServicesWithinSameSubSystem() throws Exception {
		// Lunch feeder indirectly invokes "internal" service 
		publicLunchFeeder.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		Future<LunchRestaurant> f = asyncLunchService.getLunchRestaurant(request);
		LunchRestaurant r = f.get(300, TimeUnit.MILLISECONDS);
		assertEquals("Martins Green Room", r.getName());
	}

	@Test
	public void leasesServices() throws Exception {
		AstrixServiceProperties properties = new AstrixServiceProperties();
		properties.setApi(FooService.class);
		serviceRegistryClient.register(FooService.class, properties, 1000);
		
		AstrixServiceProperties props = serviceRegistryClient.lookup(FooService.class);
		assertNotNull("Expected properties to exists after registreation", props);
		
		assertEventually(AstrixTestUtil.serviceInvocationResult(new Supplier<Object>() {
			public Object get() {
				return serviceRegistryClient.lookup(FooService.class);
			};
		}, is(nullValue())));
	}
	
	@Test(expected = IllegalSubsystemException.class)
	public void itsNotAllowedToCreateServicesBeansThatBindsToServicesInOtherSubSystems() throws Exception {
		astrix.getBean(InternalLunchFeeder.class).addLunchRestaurant(lunchRestaurant().build());;
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10_000, 10).check(probe);
	}
	
	public interface FooService {
		
	}
}
