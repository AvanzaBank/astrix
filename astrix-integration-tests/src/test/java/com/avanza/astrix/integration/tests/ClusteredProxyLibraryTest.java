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
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.IllegalSubsystemException;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchStatistics;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;

public class ClusteredProxyLibraryTest {
	
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
									  		  .contextProperty("configSourceId", config.getConfigSourceId())
											  .startAsync(true)
											  .configure();
	
	private LunchService lunchService;

	private AstrixConfigurer configurer = new AstrixConfigurer();
	private AstrixContext astrix;
	
	static {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Logger.getLogger("com.avanza.astrix").setLevel(Level.DEBUG);
	}
	
	@Before
	public void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		configurer.enableFaultTolerance(true);
		configurer.enableVersioning(true);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1000);
		configurer.set(AstrixSettings.ASTRIX_CONFIG_URI, config.getExternalConfigUri());
	}
	
	
	@Test
	public void aClusteredProxyIsConsumableUsingTheServiceRegistryFromTheSameSubsystem() throws Exception {
		configurer.setSubsystem("lunch-system");
		astrix = configurer.configure();
		this.lunchService = astrix.waitForBean(LunchService.class, 10000);
		
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		assertEquals(1, astrix.getBean(LunchStatistics.class).getRestaurantCount());
	}
	
	
	@Test(expected = IllegalSubsystemException.class)
	public void aClusteredProxyIsNotConsumableFromAnotherSubsystem() throws Exception {
		configurer.setSubsystem("another-subsystem");
		astrix = configurer.configure();
		
		astrix.getBean(LunchStatistics.class).getRestaurantCount();
	}

}
