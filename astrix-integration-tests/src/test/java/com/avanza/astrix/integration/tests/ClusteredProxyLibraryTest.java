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

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.gs.AsyncFutureTypeHandler.AsyncFutureImpl;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchStatistics;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.gs.test.junit5.PuConfigurers;
import com.avanza.gs.test.junit5.RunningPu;
import com.gigaspaces.async.AsyncFuture;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.Task;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusteredProxyLibraryTest {
	
	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry() {{
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
	}};
	
	@RegisterExtension
	static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											.numberOfPrimaries(1)
											.numberOfBackups(0)
											.contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
											.startAsync(true)
											.configure();
	
	@RegisterExtension
	AutoCloseableExtension autoClosables = new AutoCloseableExtension();
	
	private LunchService lunchService;

	private final AstrixConfigurer configurer = new AstrixConfigurer();
	private AstrixContext astrix;
	
	@BeforeEach
	void setup() {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		configurer.enableFaultTolerance(false);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1000);
		configurer.setConfig(DynamicConfig.create(serviceRegistry));
	}
	
	@Test
	void aClusteredProxyIsConsumableUsingTheServiceRegistryFromTheSameSubsystem() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
		this.lunchService = astrix.waitForBean(LunchService.class, 10000);
		
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		assertEquals(1, astrix.getBean(LunchStatistics.class).getRestaurantCount());
	}
	
	@Test
	void itsPossibleToInvokeMethodsReturningAsyncFutureWithFaultTolerance() throws Exception {
		configurer.setSubsystem("lunch-system");
		configurer.enableFaultTolerance(true);
		astrix = autoClosables.add(configurer.configure());
		GigaSpace gigaSpace = astrix.waitForBean(GigaSpace.class, "lunch-space", 10000);
		AsyncFuture<LunchRestaurant> future = gigaSpace.asyncRead(LunchRestaurant.template());
		assertEquals(AsyncFutureImpl.class, future.getClass());
	}
	
	@Test
	void aClusteredProxyIsNotConsumableFromAnotherSubsystem() {
		configurer.setSubsystem("another-subsystem");
		astrix = autoClosables.add(configurer.configure());
		
		assertThrows(ServiceUnavailableException.class, () -> astrix.getBean(LunchStatistics.class).getRestaurantCount());
	}
	
	@Test
	void aClusteredProxyUsesOptimisticLockinWhenMasterSpaceIsConfiguredForOptimisticLocking() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
		GigaSpace proxy = astrix.waitForBean(GigaSpace.class, "lunch-space", 10000);
		assertTrue(proxy.getSpace().isOptimisticLockingEnabled());
	}
	
	@Test
	void supportsMethodsReturningAsyncFuture() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
		GigaSpace proxy = astrix.waitForBean(GigaSpace.class, "lunch-space", 10000);
		AsyncFuture<Integer> result = proxy.execute(new ReturnOneTask(), 1);
		assertEquals(1, (int) result.get());
	}
	
	@Test
	void localViewTest() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
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
	
	public static class ReturnOneTask implements Task<Integer> {
		private static final long serialVersionUID = 1L;

		@Override
		public Integer execute() {
			return 1;
		}
	}
	
}
