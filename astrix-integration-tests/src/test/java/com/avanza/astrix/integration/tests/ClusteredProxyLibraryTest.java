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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Description;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchStatistics;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;

public class ClusteredProxyLibraryTest {
	
	private static InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry() {{
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
	}};
	
	@ClassRule
	public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											  .numberOfPrimaries(1)
											  .numberOfBackups(0)
									  		  .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
											  .startAsync(true)
											  .configure();
	
	private LunchService lunchService;

	private AstrixConfigurer configurer = new AstrixConfigurer();
	private AstrixContext astrix;
	
	@Before
	public void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		configurer.enableFaultTolerance(false);
		configurer.enableVersioning(true);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1000);
		configurer.setConfig(DynamicConfig.create(serviceRegistry));
	}
	
	@Test
	public void aClusteredProxyIsConsumableUsingTheServiceRegistryFromTheSameSubsystem() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = configurer.configure();
		this.lunchService = astrix.waitForBean(LunchService.class, 10000);
		
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		assertEquals(1, astrix.getBean(LunchStatistics.class).getRestaurantCount());
	}
	
	@Test(expected = ServiceUnavailableException.class)
	public void aClusteredProxyIsNotConsumableFromAnotherSubsystem() throws Exception {
		configurer.setSubsystem("another-subsystem");
		astrix = configurer.configure();
		
		astrix.getBean(LunchStatistics.class).getRestaurantCount();
	}
	
	@Test
	public void aClusteredProxyUsesOptimisticLockinWhenMasterSpaceIsConfiguredForOptimisticLocking() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = configurer.configure();
		GigaSpace proxy = astrix.waitForBean(GigaSpace.class, "lunch-space", 10000);
		assertTrue(proxy.getSpace().isOptimisticLockingEnabled());
	}
	
	
	@Test
	public void localViewTest() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = configurer.configure();
		this.lunchService = astrix.waitForBean(LunchService.class, 10_000L);
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		final GigaSpace localView = astrix.waitForBean(GigaSpace.class, "lunch-space-local-view", 10_000L);
		assertEventually(objectCount(localView, LunchRestaurant.template(), 1));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10000, 25).check(probe);
	}

	private Probe objectCount(final GigaSpace localView, final Object template, final int expected) {
		return new Probe() {
			private int count = 0;
			private Exception lastException;
			@Override
			public boolean isSatisfied() {
				return count == expected;
			}

			@Override
			public void sample() {
				lastException = null;
				try {
					count = localView.count(template);
				} catch (Exception e) {
					lastException = e;
				}
			}

			@Override
			public void describeFailureTo(Description description) {
				if (lastException != null) {
					lastException.printStackTrace(); // for debug purpose
					description.appendText("Object count for template: " + template.toString() + "\n expected: " + expected + " \n But last invocation threw exception: " + lastException.toString());
				}
				description.appendText("Object count for template: " + template.toString() + "\n expected: " + expected + " \n was: " + count);
			}
			
		};
	}
	
}
