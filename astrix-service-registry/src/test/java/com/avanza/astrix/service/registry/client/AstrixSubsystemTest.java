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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.avanza.astrix.beans.registry.AstrixServiceRegistryClient;
import com.avanza.astrix.beans.service.IllegalSubsystemException;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.provider.versioning.Versioned;
import com.avanza.astrix.service.registry.util.InMemoryServiceRegistry;

public class AstrixSubsystemTest {
	
	@Test(expected = IllegalSubsystemException.class)
	public void itsNotAllowedToInvokeNonVersionedServicesInOtherSubsystems() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		// Simulate two different subsystems using to AstrixContextImpl's
		TestAstrixConfigurer configurerA = new TestAstrixConfigurer();
		configurerA.setSubsystem("A");
		configurerA.registerApiProvider(GreetingServiceProvider.class);
		configurerA.registerAstrixBean(AstrixServiceRegistry.class, serviceRegistry);
		configurerA.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		AstrixContext contextA = configurerA.configure();
		
		TestAstrixConfigurer configurerB = new TestAstrixConfigurer();
		configurerB.setSubsystem("B");
		configurerB.registerApiProvider(GreetingServiceProvider.class);
		configurerB.registerAstrixBean(AstrixServiceRegistry.class, serviceRegistry);
		configurerB.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		AstrixContext contextB = configurerB.configure();

		// Publish non versioned service in contextB
		AstrixServiceRegistryClient serviceRegistryClientB = contextB.getBean(AstrixServiceRegistryClient.class);
		serviceRegistryClientB.register(GreetingService.class, AstrixDirectComponent.registerAndGetProperties(GreetingService.class, new GreetingServiceImpl("hello")), 1000);
		
		GreetingService greetingServiceB = contextB.getBean(GreetingService.class);
		
		assertNotNull("Its allowed to invoce service provided in same subsystem", greetingServiceB.hello("foo"));
		
		GreetingService greetingServiceA = contextA.getBean(GreetingService.class);
		
		// It should not be allowed to invoke this service since its in subsystem A
		greetingServiceA.hello("bar");
	}
	
	@Test
	public void itsAllowedToInvokeVersionedServicesInOtherSubsystems() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		// Simulate two different subsystems using to AstrixContextImpl's
		TestAstrixConfigurer configurerA = new TestAstrixConfigurer();
		configurerA.setSubsystem("A");
		configurerA.registerApiProvider(VersionedGreetingServiceProvider.class);
		configurerA.registerAstrixBean(AstrixServiceRegistry.class, serviceRegistry);
		configurerA.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		AstrixContext contextA = configurerA.configure();
		
		TestAstrixConfigurer configurerB = new TestAstrixConfigurer();
		configurerB.setSubsystem("B");
		configurerB.registerApiProvider(VersionedGreetingServiceProvider.class);
		configurerB.registerAstrixBean(AstrixServiceRegistry.class, serviceRegistry);
		configurerB.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		AstrixContext contextB = configurerB.configure();

		// Publish non versioned service in contextB
		AstrixServiceRegistryClient serviceRegistryClientB = contextB.getBean(AstrixServiceRegistryClient.class);
		serviceRegistryClientB.register(GreetingService.class, AstrixDirectComponent.registerAndGetProperties(GreetingService.class, new GreetingServiceImpl("hello")), 1000);
		
		GreetingService greetingServiceB = contextB.getBean(GreetingService.class);
		
		assertNotNull("Its allowed to invoce service provided in same subsystem", greetingServiceB.hello("foo"));
		
		GreetingService greetingServiceA = contextA.getBean(GreetingService.class);
		
		// It should be allowed to invoke this service since its versioned
		greetingServiceA.hello("bar");
	}
	
	@AstrixApiProvider
	interface GreetingServiceProvider {
		@Service
		GreetingService greetingService();
	}
	
	@AstrixObjectSerializerConfig(
		objectSerializerConfigurer = DummyConfigurer.class,
		version = 1
	)
	@AstrixApiProvider
	interface VersionedGreetingServiceProvider {
		@Versioned
		@Service
		GreetingService greetingService();
	}
	
	public static class DummyConfigurer implements AstrixObjectSerializerConfigurer {
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
