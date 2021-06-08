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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import com.avanza.gs.test.junit5.PuConfigurers;
import com.avanza.gs.test.junit5.RunningPu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class ServiceRegistryPuIntegrationTest {
	
	@RegisterExtension
	RunningPu serviceRegistryPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
													  .numberOfPrimaries(1)
													  .numberOfBackups(0)
													  .startAsync(false)
													  .configure();
	
	private final MapConfigSource clientConfig = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistryPu.getLookupGroupName());
	}};
	
	private AstrixContext clientContext;
	
	@RegisterExtension
	AutoCloseableExtension autoCloseableExtension = new AutoCloseableExtension();
	
	@BeforeEach
	void setup() {
		this.clientContext = autoCloseableExtension.add(new AstrixConfigurer().setConfig(DynamicConfig.create(clientConfig)).configure());
	}
	
	@Test
	void serviceRegistration() {
		AstrixServiceRegistry serviceRegistry = clientContext.getBean(AstrixServiceRegistry.class);
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		ServiceRegistryExporterClient exporterClient1 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-1");
		ServiceRegistryExporterClient exporterClient2 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-2");
		
		ServiceProperties server1Props = new ServiceProperties();
		server1Props.getProperties().put("myProp", "1");
		ServiceProperties server2 = new ServiceProperties();
		server2.getProperties().put("myProp", "1");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		exporterClient2.register(SomeService.class, server2, 10000);
		exporterClient2.register(AnotherService.class, new ServiceProperties(), 10000);
		
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		
		server1Props = new ServiceProperties();
		server1Props.getProperties().put("myProp", "3");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		
		providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		ServiceProperties serviceProperties = getPropertiesForAppInstance("app-instance-1", providers);
		assertEquals("3", serviceProperties.getProperty("myProp"));
	}
	
	private ServiceProperties getPropertiesForAppInstance(String appInstanceId, List<ServiceProperties> providers) {
		for (ServiceProperties properties : providers) {
			if (appInstanceId.equals(properties.getProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID))) {
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
