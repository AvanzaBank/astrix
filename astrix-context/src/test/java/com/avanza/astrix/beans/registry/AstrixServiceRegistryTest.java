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
package com.avanza.astrix.beans.registry;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;

public class AstrixServiceRegistryTest {
	
	InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	AstrixContext clientContext;
	
	public void after() {
		AstrixTestUtil.closeSafe(clientContext);
	}
	
	@Test
	public void serviceRegsistrySupportsMultipleProvidersOfSameService() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.setSubsystem("default");
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		server1serviceRegistryClient.register(Ping.class, AstrixDirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1")), Integer.MAX_VALUE);
		
		
		ServiceRegistryExporterClient server2serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-2");
		server2serviceRegistryClient.register(Ping.class, AstrixDirectComponent.registerAndGetProperties(Ping.class, new PingImpl("2")), Integer.MAX_VALUE);
		
		
		Ping ping1 = clientContext.getBean(Ping.class);
		AstrixServiceRegistryClient serviceRegistryClient = clientContext.getBean(AstrixServiceRegistryClient.class);
		List<AstrixServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(2, providers.size());
		assertNotNull(ping1.ping());
	}
	
	@Test
	public void doesNotBindToInactiveProviders() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.setSubsystem("default");
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		AstrixServiceProperties service1Properties = AstrixDirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		service1Properties.setProperty(AstrixServiceProperties.SERVICE_STATE, ServiceState.INACTIVE);
		server1serviceRegistryClient.register(Ping.class, service1Properties, Integer.MAX_VALUE);
		
		AstrixServiceRegistryClient serviceRegistryClient = clientContext.getBean(AstrixServiceRegistryClient.class);
		List<AstrixServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(1, providers.size());

		Ping ping = clientContext.getBean(Ping.class);
		try {
			ping.ping();
			fail("Expected service to not be available when server is INACTIVE");
		} catch (ServiceUnavailableException e) {
			// expected
		}
	}
	
	@Test
	public void usesRoundRobinToDistributeConsumers() throws Exception {
		BasicConfigurator.configure();
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		AstrixServiceProperties service1Properties = AstrixDirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		server1serviceRegistryClient.register(Ping.class, service1Properties, Integer.MAX_VALUE);
		
		ServiceRegistryExporterClient server2serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-2");
		AstrixServiceProperties service2Properties = AstrixDirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		server2serviceRegistryClient.register(Ping.class, service2Properties, Integer.MAX_VALUE);
		
		AstrixServiceRegistryClient serviceRegistryClient = clientContext.getBean(AstrixServiceRegistryClient.class);
		int server1ConsumerCount = 0;
		int server2ConsumerCount = 0;
		for (int i = 0; i < 10; i++) {
			AstrixServiceProperties props = serviceRegistryClient.lookup(AstrixBeanKey.create(Ping.class));
			if ("server-1".equals(props.getProperty(AstrixServiceProperties.APPLICATION_INSTANCE_ID))) {
				server1ConsumerCount++;
			}
			if ("server-2".equals(props.getProperty(AstrixServiceProperties.APPLICATION_INSTANCE_ID))) {
				server2ConsumerCount++;
			}
		}
		
		assertEquals(5, server1ConsumerCount);
		assertEquals(5, server2ConsumerCount);
		
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping();
	}
	
	public class PingImpl implements Ping {
		private String id;
		
		public PingImpl(String id) {
			this.id = id;
		}

		@Override
		public String ping() {
			return id;
		}
		
	}

}
