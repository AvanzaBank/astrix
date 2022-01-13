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

import static com.avanza.astrix.gs.ClusteredProxyCacheUtil.getObjectCache;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

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
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.gs.ClusteredProxyCacheImpl.GigaSpaceInstance;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchServiceAsync;
import com.avanza.astrix.modules.ObjectCache;
import com.avanza.gs.test.JVMGlobalLus;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;

public class RestartSpaceTest {
	private final String lookupGroupName = JVMGlobalLus.getLookupGroupName();
	private final String spaceName = this.getClass().getName();
	private final String puSpaceUrl = "jini://*/*/" + spaceName + "?groups=" + lookupGroupName;
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
		contextProperties.put("spaceName", spaceName);
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

	private ClusteredProxyCache getAstrixInternalClusteredProxyCache() {
		return ((AstrixApplicationContext) this.astrix).getInstance(ClusteredProxyCache.class);
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

	@Test
	public void shouldRecreateSpaceProxyWhenPuHasRestarted() throws Exception {
		// Arrange
		pu = startPuContainer();
		astrix = new AstrixConfigurer().setConfig(dynamicConfig).configure();
		astrix.waitForBean(LunchService.class, 5000);
		astrix.waitForBean(LunchServiceAsync.class, 5000);
		ObjectCache objectCache = getObjectCache(getAstrixInternalClusteredProxyCache());
		// one gs proxy to the service registry
		// one gs proxy to the space that exposes LunchService and LunchServiceAsync
		assertEquals(2, objectCache.getCurrentSize());
		assertEquals(2, objectCache.getCreatedCount());

		// Act
		pu.close();
		pu = startPuContainer();
		// Wait for service renewal to execute
		Thread.sleep(1_000);

		// Assert
		assertEquals(2, objectCache.getCurrentSize());
		// This is the important part of the test: We want to make sure that
		// three gs connections have been created
		//  - one to the service registry,
		//  - one for the first LunchService service bean instance (before restart)
		//  - and a third one for the re-bound service bean instance (after pu restart)
		// There was a problem here earlier when both LunchService and
		// LunchServiceAsync used the same space url, which made the service
		// bean instances re-use the same cached instance of the gs proxy.
		assertEquals(3, objectCache.getCreatedCount());
	}

	@Test
	public void shouldGetSameGigaSpacesInstanceIfServicePropertiesPointToSameSpace() throws Exception {
		// Arrange
		pu = startPuContainer();
		astrix = new AstrixConfigurer().setConfig(dynamicConfig).configure();
		ClusteredProxyCache clusteredProxyCache = getAstrixInternalClusteredProxyCache();

		// Two services with different api:s but with the same space url
		final ServiceProperties properties1 = new ServiceProperties();
		properties1.setApi(LunchService.class);
		properties1.setProperty(GsBinder.SPACE_URL_PROPERTY, puSpaceUrl);
		final ServiceProperties properties2 = new ServiceProperties();
		properties2.setApi(LunchServiceAsync.class);
		properties2.setProperty(GsBinder.SPACE_URL_PROPERTY, puSpaceUrl);

		// Act
		GigaSpaceInstance instance1 = clusteredProxyCache.getProxy(properties1);
		GigaSpaceInstance instance2 = clusteredProxyCache.getProxy(properties2);

		// Assert
		assertSame(instance1, instance2);
		instance1.release();
		instance2.release();
	}

	@Test
	public void shouldGetNewGigaSpacesInstanceIfStartTimeHasChanged() throws Exception {
		// Arrange
		pu = startPuContainer();
		astrix = new AstrixConfigurer().setConfig(dynamicConfig).configure();
		ClusteredProxyCache clusteredProxyCache = getAstrixInternalClusteredProxyCache();

		// Two services with same api and space url but with different start time.
		// Having a different start time simulates that the PU has restarted.
		final ServiceProperties properties1 = new ServiceProperties();
		properties1.setApi(LunchService.class);
		properties1.setProperty(GsBinder.SPACE_URL_PROPERTY, puSpaceUrl);
		properties1.setProperty(GsBinder.START_TIME, "10");
		final ServiceProperties properties2 = new ServiceProperties();
		properties2.setApi(LunchService.class);
		properties2.setProperty(GsBinder.SPACE_URL_PROPERTY, puSpaceUrl);
		properties2.setProperty(GsBinder.START_TIME, "20");

		// Act
		GigaSpaceInstance instance1 = clusteredProxyCache.getProxy(properties1);
		GigaSpaceInstance instance2 = clusteredProxyCache.getProxy(properties2);

		// Assert
		assertNotSame(instance1, instance2);
		instance1.release();
		instance2.release();
	}
}
