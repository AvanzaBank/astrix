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
package com.avanza.astrix.service.registry.client;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryServiceProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.DirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;


public class DefaultServiceLookupMethodTest {
	
	private static final long UNUSED_LEASE = 10_000L;
	private AstrixContext context;
	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@Before
	public void setup() {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.registerApiProvider(GreetingApiProviderNoLookupDefined.class);
		configurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		configurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		configurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		context = configurer.configure();
	}
	
	@Test
	public void lookupService_serviceAvailableInRegistry_ServiceIsImmediatlyBound() throws Exception {
		final String objectId = DirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		
		ServiceRegistryExporterClient serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "FooInstanceId");
		serviceRegistryClient.register(GreetingService.class, DirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}

	@AstrixApiProvider
	interface GreetingApiProviderNoLookupDefined {
		
		@Service
		GreetingService greetingService();
	}
	
	public interface GreetingService {
		String hello(String msg);
	}
	
	public static class GreetingServiceImpl implements GreetingService {
		
		private String greeting;

		public GreetingServiceImpl(String greeting) {
			this.greeting = greeting;
		}
		
		@Override
		public String hello(String msg) {
			return greeting + msg;
		}
	}
	

}
