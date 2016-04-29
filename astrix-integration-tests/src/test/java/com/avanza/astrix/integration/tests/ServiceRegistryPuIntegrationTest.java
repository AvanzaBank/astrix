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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryPuIntegrationTest {
	
	
	
	@Rule
	public RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
															.numberOfPrimaries(1)
															.numberOfBackups(0)
															.startAsync(false)
															.configure();
	
	private MapConfigSource clientConfig = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
	}};
	
	private AstrixContext clientContext;
	
	@Rule
	public AutoCloseableRule autoCloseableRule = new AutoCloseableRule();
	
	@Before
	public void setup() throws Exception {
		this.clientContext = autoCloseableRule.add(new AstrixConfigurer().setConfig(DynamicConfig.create(clientConfig)).configure());
	}
	
	@Test
	public void serviceRegistration() throws Exception {
		AstrixServiceRegistry serviceRegistry = clientContext.getBean(AstrixServiceRegistry.class);
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		ServiceRegistryExporterClient exporterClient1 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-1");
		ServiceRegistryExporterClient exporterClient2 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-2");
		
		ServiceProviderInstanceProperties server1Props = new ServiceProviderInstanceProperties();
		server1Props.getProperties().put("myProp", "1");
		ServiceProviderInstanceProperties server2 = new ServiceProviderInstanceProperties();
		server2.getProperties().put("myProp", "1");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		exporterClient2.register(SomeService.class, server2, 10000);
		exporterClient2.register(AnotherService.class, new ServiceProviderInstanceProperties(), 10000);
		
		List<ServiceProviderInstanceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		
		server1Props = new ServiceProviderInstanceProperties();
		server1Props.getProperties().put("myProp", "3");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		
		providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		ServiceProviderInstanceProperties serviceProperties = getPropertiesForAppInstance("app-instance-1", providers);
		assertEquals("3", serviceProperties.getProperty("myProp"));
	}
	
	private ServiceProviderInstanceProperties getPropertiesForAppInstance(String appInstanceId, List<ServiceProviderInstanceProperties> providers) {
		for (ServiceProviderInstanceProperties properties : providers) {
			if (appInstanceId.equals(properties.getProperties().get(ServiceProviderInstanceProperties.APPLICATION_INSTANCE_ID))) {
				return properties;
			}
		}
		return null;
	}

	interface SomeService {
		
	}
	
	interface AnotherService {
	}
	
}
