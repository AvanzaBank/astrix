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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.gs.test.JVMGlobalLus;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;
import com.gigaspaces.admin.quiesce.DefaultQuiesceToken;
import com.gigaspaces.admin.quiesce.QuiesceException;
import com.gigaspaces.admin.quiesce.QuiesceState;
import com.gigaspaces.admin.quiesce.QuiesceStateChangedEvent;

public class AstrixServiceUnavailableIntegrationTest {
	private final String lookupGroupName = JVMGlobalLus.getLookupGroupName();
	private final MapConfigSource configSource = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, "gs-remoting:jini://*/*/service-registry-space?groups=" + lookupGroupName);
		set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
	}};
	private final DynamicConfig dynamicConfig = new DynamicConfig(configSource);
	private final RunningPu serviceRegistryPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
			.lookupGroup(lookupGroupName)
			.startAsync(false)
			.configure();
	private final RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
			.contextProperty("configSourceId", GlobalConfigSourceRegistry.register(configSource))
			.lookupGroup(lookupGroupName)
			.startAsync(false)
			.configure();

	@Rule
	public RuleChain ruleChain = RuleChain.emptyRuleChain()
			.around(serviceRegistryPu)
			.around(lunchPu);

	private AstrixContext astrix;
	private LunchService lunchService;

	@Before
	public void beforeEachTest() throws Exception {
		astrix = new AstrixConfigurer().setConfig(dynamicConfig).configure();
		lunchService = astrix.waitForBean(LunchService.class, 5000);
	}

	@After
	public void afterEachTest() throws Exception {
		if (astrix != null) {
			astrix.close();
		}
	}

	private void setSpaceInQuiesceMode(GigaSpace space) {
		space.getSpace().getDirectProxy().getSpaceImplIfEmbedded().getQuiesceHandler().setQuiesceMode(
				new QuiesceStateChangedEvent(
						QuiesceState.QUIESCED,
						new DefaultQuiesceToken("quiesce_token"),
						"description"
				)
		);
	}

	@Test
	public void shouldFailToCallBroadcastAstrixServiceOnQuiescedSpace() {
		// Arrange
		setSpaceInQuiesceMode(lunchPu.getClusteredGigaSpace());

		try {
			// Act
			lunchService.suggestRandomLunchRestaurant("request");

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (ServiceUnavailableException e) {
			assertThat(e.toString(), containsString("Operation cannot be executed on a quiesced space"));
			assertThat(e.getCause(), instanceOf(QuiesceException.class));
			assertThat(e.getCause().getCause(), instanceOf(AstrixCallStackTrace.class));
		}
	}

	@Test
	public void shouldFailToCallPartitionedRoutedAstrixServiceOnQuiescedSpace() {
		// Arrange
		setSpaceInQuiesceMode(lunchPu.getClusteredGigaSpace());

		try {
			// Act
			lunchService.getLunchRestaurants("request");

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (ServiceUnavailableException e) {
			assertThat(e.toString(), containsString("Operation cannot be executed on a quiesced space"));
			assertThat(e.getCause(), instanceOf(QuiesceException.class));
			assertThat(e.getCause().getCause(), instanceOf(AstrixCallStackTrace.class));
		}
	}

	@Test
	public void shouldFailToCallSingleRoutedAstrixServiceOnQuiescedSpace() {
		// Arrange
		setSpaceInQuiesceMode(lunchPu.getClusteredGigaSpace());

		try {
			// Act
			lunchService.getLunchRestaurant(new GetLunchRestaurantRequest("request"));

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (ServiceUnavailableException e) {
			assertThat(e.toString(), containsString("Operation cannot be executed on a quiesced space"));
			assertThat(e.getCause(), instanceOf(QuiesceException.class));
			assertThat(e.getCause().getCause(), instanceOf(AstrixCallStackTrace.class));
		}
	}
}
