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
package com.avanza.astrix.integration.tests;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.integration.tests.common.Ping;
import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchServiceAsync;
import com.avanza.astrix.integration.tests.domain.api.LunchUtil;
import com.avanza.astrix.integration.tests.domain.apiruntime.feeder.InternalLunchFeeder;
import com.avanza.astrix.integration.tests.domain.pu.LunchApplicationDescriptor;
import com.avanza.astrix.integration.tests.domain2.api.LunchRestaurantGrader;
import com.avanza.astrix.integration.tests.domain2.apiruntime.PublicLunchFeeder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;

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
	
	private static MapConfigSource config = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
		set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
	}};
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											  .numberOfPrimaries(1)
											  .numberOfBackups(0)
											  .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
											  .startAsync(true)
											  .configure();
	
	@ClassRule
	public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
														.numberOfPrimaries(1)
														.numberOfBackups(0)
														  .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
														.startAsync(true)
														.configure();
	
	@Rule 
	public AutoCloseableRule autoClosables = new AutoCloseableRule();

	private LunchService lunchService;
	private LunchUtil lunchUtil;
	private LunchRestaurantGrader lunchRestaurantGrader;
	private LunchServiceAsync asyncLunchService;
	private PublicLunchFeeder publicLunchFeeder;
	private AstrixContext astrix;
	private ServiceRegistryClient serviceRegistryClient;

	private Ping lunchPing;

	private AstrixServiceRegistry serviceRegistry;

	@Before
	public void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.enableFaultTolerance(true);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
		configurer.setConfig(DynamicConfig.create(config));
		configurer.setSubsystem("test-sub-system");
		astrix = autoClosables.add(configurer.configure());
		this.lunchService = astrix.getBean(LunchService.class);
		this.lunchUtil = astrix.getBean(LunchUtil.class);
		this.lunchRestaurantGrader = astrix.getBean(LunchRestaurantGrader.class);
		this.asyncLunchService = astrix.getBean(LunchServiceAsync.class);
		this.publicLunchFeeder = astrix.getBean(PublicLunchFeeder.class);
		this.serviceRegistryClient = astrix.getBean(ServiceRegistryClient.class);
		this.serviceRegistry = astrix.getBean(AstrixServiceRegistry.class);
		this.lunchPing = astrix.getBean(Ping.class, "lunch-ping");
		astrix.waitForBean(LunchService.class, 5000);
		astrix.waitForBean(AstrixServiceRegistry.class, 5000);
		astrix.waitForBean(LunchUtil.class, 5000);
		astrix.waitForBean(LunchRestaurantGrader.class, 5000);
		astrix.waitForBean(LunchServiceAsync.class, 5000);
		astrix.waitForBean(PublicLunchFeeder.class, 5000);
		astrix.waitForBean(AstrixServiceRegistry.class, 5000);
		astrix.waitForBean(Ping.class, "lunch-ping", 5000);
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
	public void partitionedRemotingRequest() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		lunchService.addLunchRestaurant(lunchRestaurant().withName("McDonalds").build());
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Max Burger").build());
		
		List<LunchRestaurant> r = lunchService.getLunchRestaurants("McDonalds", "Max Burger");
		assertEquals(2, r.size());
		assertThat(r, hasItem(restaurantWithName("McDonalds")));
		assertThat(r, hasItem(restaurantWithName("Max Burger")));
		
		// It should be possible to send "empty" requests
		r = lunchService.getLunchRestaurants();
		assertEquals(0, r.size());
		
	}
	
	@Test
	public void requestToQualifiedService() throws Exception {
		assertEquals("hi", lunchPing.ping("hi"));
	}
	
	@Test
	public void testPuThatConsumesAnotherService() throws Exception {
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		lunchRestaurantGrader.grade("Martins Green Room", 2);
		lunchRestaurantGrader.grade("Martins Green Room", 4);
		
		assertEquals(3.0, lunchRestaurantGrader.getAvarageGrade("Martins Green Room"), 0.01D);
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
		} catch (RemoteServiceInvocationException e) {
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
		ServiceProperties properties = new ServiceProperties();
		properties.setApi(FooService.class);
		ServiceRegistryExporterClient exporterClient = new ServiceRegistryExporterClient(serviceRegistry, "test-sub-system" , "foo-app-instance-id");
		exporterClient.register(FooService.class, properties, 1000);
		
		ServiceProperties props = serviceRegistryClient.lookup(AstrixBeanKey.create(FooService.class));
		assertNotNull("Expected properties to exists after registration", props);
		
		assertEventually(AstrixTestUtil.serviceInvocationResult(new Supplier<Object>() {
			public Object get() {
				return serviceRegistryClient.lookup(AstrixBeanKey.create(FooService.class));
			};
		}, is(nullValue())));
	}
	
	@Test
	public void usesSpaceApplicationDescriptorNameAsdefaultApplicationInstanceIdForProcessingUnits() throws Exception {
		ServiceProperties serviceProperties = serviceRegistryClient.lookup(AstrixBeanKey.create(LunchService.class));
		assertEquals(LunchApplicationDescriptor.class.getName(), serviceProperties.getProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID));
	}
	
	@Test(expected = ServiceUnavailableException.class)
	public void itsNotPossibleToBindToNonPublishedServiceBeansProvidedByOtherSubsystems() throws Exception {
		astrix.getBean(InternalLunchFeeder.class).addLunchRestaurant(lunchRestaurant().build());
	}

	private TypeSafeMatcher<LunchRestaurant> restaurantWithName(final String restaurantName) {
		return new TypeSafeMatcher<LunchRestaurant>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("LunchRestaurant with name: " + restaurantName);
			}

			@Override
			protected boolean matchesSafely(LunchRestaurant item) {
				return item.getName().equals(restaurantName);
			}
		};
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10_000, 10).check(probe);
	}
	
	public interface FooService {
		
	}
}
