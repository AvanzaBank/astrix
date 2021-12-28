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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.properties.BeanLevelProperties;
import org.openspaces.pu.container.ProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.gs.test.JVMGlobalLus;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;

public class RestartSpaceTest {
	private final String lookupGroupName = JVMGlobalLus.getLookupGroupName();
	private final MapConfigSource configSource = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, "gs-remoting:jini://*/*/service-registry-space?groups=" + lookupGroupName);
		set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
		set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_INTERVAL, 150);
	}};
	private final String configSourceId = GlobalConfigSourceRegistry.register(configSource);
	private final DynamicConfig dynamicConfig = new DynamicConfig(configSource);
	@Rule
	public final RunningPu serviceRegistryPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
			.lookupGroup(lookupGroupName)
			.startAsync(false)
			.configure();
	private AstrixContext astrix;
	private ProcessingUnitContainer pu;

	@After
	public void afterEachTest() throws Exception {
		if (astrix != null) {
			astrix.close();
		}
		if (pu != null) {
			pu.close();
		}
	}

	private ProcessingUnitContainer startPuContainer() throws IOException {
		Properties contextProperties = new Properties();
		contextProperties.put("spaceName", this.getClass().getName());
		contextProperties.put("configSourceId", configSourceId);
		contextProperties.put("gs.space.url.arg.groups", this.lookupGroupName);
		BeanLevelProperties beanLevelProperties = new BeanLevelProperties();
		beanLevelProperties.setContextProperties(contextProperties);

		// Cannot use "PuConfigurers.partitionedPu" for this space since it
		// gives a unique spacename for each start, and we want to restart with
		// the same spacename in these tests.
		IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
		provider.setClusterInfo(new ClusterInfo("partitioned", 1, 0, 1, 0));
		provider.addConfigLocation("classpath:/META-INF/spring/lunch-pu.xml");
		provider.setBeanLevelProperties(beanLevelProperties);
		return provider.createContainer();
	}

	@Test
	public void shouldCallAstrixServiceWithoutExceptionsWhenPuHasRestarted() throws Exception {
		// Arrange
		pu = startPuContainer();
		astrix = new AstrixConfigurer().setConfig(dynamicConfig).configure();
		LunchService lunchService = astrix.waitForBean(LunchService.class, 5000);
		List<LunchRestaurant> response1 = lunchService.getLunchRestaurants("request");

		// Act
		pu.close();
		pu = startPuContainer();
		// Wait for service renewal to execute
		Thread.sleep(1_000);

		// Assert
		List<LunchRestaurant> response2 = lunchService.getLunchRestaurants("request");
		assertEquals(response1, response2);
	}
}
