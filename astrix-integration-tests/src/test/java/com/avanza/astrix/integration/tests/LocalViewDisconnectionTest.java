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
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.gs.test.junit5.PuConfigurers;
import com.avanza.gs.test.junit5.RunningPu;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openspaces.core.GigaSpace;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;

class LocalViewDisconnectionTest {
	
	private final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry() {{
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
	}};
	
	private LunchService lunchService;

	private final AstrixConfigurer configurer = new AstrixConfigurer();
	private AstrixContext astrix;
	
	@RegisterExtension
	AutoCloseableExtension autoClosables = new AutoCloseableExtension();
	
	@RegisterExtension
	RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
									 .numberOfPrimaries(1)
									 .numberOfBackups(0)
									 .spaceName("local-view-disonnection-test")
									 .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
									 .startAsync(false)
									 .configure();
	
	@BeforeEach
	void setup() {
		configurer.enableFaultTolerance(false);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 100);
		configurer.set(AstrixSettings.GS_LOCAL_VIEW_MAX_DISCONNECTION_TIME, 2000L);
		configurer.setConfig(DynamicConfig.create(serviceRegistry));
		configurer.setSubsystem("lunch-system");
		astrix = autoClosables.add(configurer.configure());
	}
	
	@Test
	void localViewDisconnectionTest() throws Exception {
		this.lunchService = astrix.waitForBean(LunchService.class, 10_000L);
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		final GigaSpace localView = astrix.waitForBean(GigaSpace.class, "lunch-space-local-view", 10_000L);
		assertEventually(objectCount(localView, LunchRestaurant.template(), 1));
		
		stopPu();
		
		assertEventually(AstrixTestUtil.serviceInvocationException(
				() -> localView.count(LunchRestaurant.template()), 
				AstrixTestUtil.isExceptionOfType(ServiceUnavailableException.class)));


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
