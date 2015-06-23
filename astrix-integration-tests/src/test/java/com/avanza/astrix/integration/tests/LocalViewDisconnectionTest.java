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

import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;

@Ignore
public class LocalViewDisconnectionTest {
	
	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry() {{
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
	}};
	
	private LunchService lunchService;

	private AstrixConfigurer configurer = new AstrixConfigurer();
	private AstrixContext astrix;
	
	@Rule
	public AutoCloseableRule autoClosables = new AutoCloseableRule();
	
	@Rule
	public RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
											  .numberOfPrimaries(1)
											  .numberOfBackups(0)
											  .spaceName("local-view-disonnection-test")
									  		  .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
											  .startAsync(false)
											  .configure();
	
	@Before	
	public void setup() throws Exception {
		configurer.enableFaultTolerance(false);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 100);
		configurer.setConfig(DynamicConfig.create(serviceRegistry));
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
	}
	
	@Test
	public void localViewDisonnectionTest() throws Exception {
		this.lunchService = astrix.waitForBean(LunchService.class, 10_000L);
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		final GigaSpace localView = astrix.waitForBean(GigaSpace.class, "lunch-space-local-view", 10_000L);
		assertEventually(objectCount(localView, LunchRestaurant.template(), 1));
		
		stopPu();
		
		assertEventually(AstrixTestUtil.serviceInvocationException(
				new Supplier<Integer>() {
					@Override
					public Integer get() {
						return localView.count(LunchRestaurant.template());
					}
				}, AstrixTestUtil.isExceptionOfType(ServiceUnavailableException.class)));

		startPu();
		assertEventually(objectCount(localView, LunchRestaurant.template(), 0));
	}

	private void startPu() throws Exception {
		lunchPu.start();
	}

	private void stopPu() throws Exception {
		lunchPu.stop();
	}

	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10000, 100).check(probe);
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
