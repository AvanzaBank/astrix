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
package com.avanza.astrix.service.registry.client;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixServiceProvider;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.service.registry.util.InMemoryServiceRegistry;


public class AstrixServiceRegistryLookupTest {
	
	private static final long UNUSED_LEASE = 10_000L;
	private AstrixServiceRegistryClient serviceRegistryClient;
	private AstrixContext context;
	private InMemoryServiceRegistry fakeServiceRegistry;
	
	static {
	// 	TODO: remove debugging information
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Logger.getLogger("com.avanza.astrix").setLevel(Level.DEBUG);
	}
	
	@Before
	public void setup() {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiDescriptor(GreetingApiDescriptor.class);
		configurer.registerApiDescriptor(InMemoryServiceRegistryDescriptor.class);
		configurer.registerApiDescriptor(AstrixServiceRegistryLibrary.class);
		context = configurer.configure();
		fakeServiceRegistry = (InMemoryServiceRegistry) context.getBean(AstrixServiceRegistry.class);
		serviceRegistryClient = context.getBean(AstrixServiceRegistryClient.class);
	}
	
	@Test
	public void lookupService_serviceAvailableInRegistry_ServiceIsImmediatlyBound() throws Exception {
		final String objectId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	@AstrixServiceRegistryLookup
	@AstrixServiceProvider(GreetingService.class)
	public static class GreetingApiDescriptor {
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
