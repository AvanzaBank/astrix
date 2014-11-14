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
package com.avanza.asterix.service.registry.client;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.avanza.asterix.context.AsterixContext;
import com.avanza.asterix.context.AsterixDirectComponent;
import com.avanza.asterix.context.IllegalSubsystemException;
import com.avanza.asterix.context.TestAsterixConfigurer;
import com.avanza.asterix.provider.core.AsterixServiceRegistryApi;
import com.avanza.asterix.provider.versioning.AsterixObjectMapperConfigurer;
import com.avanza.asterix.provider.versioning.AsterixVersioned;
import com.avanza.asterix.provider.versioning.JacksonObjectMapperBuilder;
import com.avanza.asterix.service.registry.util.InMemoryServiceRegistry;

public class AsterixSubsystemTest {

	@Test(expected = IllegalSubsystemException.class)
	public void itsNotAllowedToInvokeNonVersionedServicesInOtherSubsystems() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		// Simulate two different subsystems using to AsterixContext's
		TestAsterixConfigurer configurerA = new TestAsterixConfigurer();
		configurerA.setSubsystem("A");
		configurerA.registerApiDescriptor(GreetingApiDescriptor.class);
		configurerA.registerApi(AsterixServiceRegistry.class, serviceRegistry);
		configurerA.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		AsterixContext contextA = configurerA.configure();
		
		TestAsterixConfigurer configurerB = new TestAsterixConfigurer();
		configurerB.setSubsystem("B");
		configurerB.registerApiDescriptor(GreetingApiDescriptor.class);
		configurerB.registerApi(AsterixServiceRegistry.class, serviceRegistry);
		configurerB.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		AsterixContext contextB = configurerB.configure();

		// Publish non versioned service in contextB
		AsterixServiceRegistryClient serviceRegistryClientB = contextB.getBean(AsterixServiceRegistryClient.class);
		serviceRegistryClientB.register(GreetingService.class, AsterixDirectComponent.registerAndGetProperties(GreetingService.class, new GreetingServiceImpl("hello")), 1000);
		
		GreetingService greetingServiceB = contextB.getBean(GreetingService.class);
		
		assertNotNull("Its allowed to invoce service provided in same subsystem", greetingServiceB.hello("foo"));
		
		GreetingService greetingServiceA = contextA.getBean(GreetingService.class);
		
		// It should not be allowed to invoke this service since its in subsystem A
		greetingServiceA.hello("bar");
	}
	
	@Test
	public void itsAllowedToInvokeVersionedServicesInOtherSubsystems() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		// Simulate two different subsystems using to AsterixContext's
		TestAsterixConfigurer configurerA = new TestAsterixConfigurer();
		configurerA.setSubsystem("A");
		configurerA.registerApiDescriptor(VersionedGreetingApiDescriptor.class);
		configurerA.registerApi(AsterixServiceRegistry.class, serviceRegistry);
		configurerA.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		AsterixContext contextA = configurerA.configure();
		
		TestAsterixConfigurer configurerB = new TestAsterixConfigurer();
		configurerB.setSubsystem("B");
		configurerB.registerApiDescriptor(VersionedGreetingApiDescriptor.class);
		configurerB.registerApi(AsterixServiceRegistry.class, serviceRegistry);
		configurerB.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		AsterixContext contextB = configurerB.configure();

		// Publish non versioned service in contextB
		AsterixServiceRegistryClient serviceRegistryClientB = contextB.getBean(AsterixServiceRegistryClient.class);
		serviceRegistryClientB.register(GreetingService.class, AsterixDirectComponent.registerAndGetProperties(GreetingService.class, new GreetingServiceImpl("hello")), 1000);
		
		GreetingService greetingServiceB = contextB.getBean(GreetingService.class);
		
		assertNotNull("Its allowed to invoce service provided in same subsystem", greetingServiceB.hello("foo"));
		
		GreetingService greetingServiceA = contextA.getBean(GreetingService.class);
		
		// It should be allowed to invoke this service since its versioned
		greetingServiceA.hello("bar");
	}
	
	@AsterixServiceRegistryApi(exportedApis = GreetingService.class)
	public static class GreetingApiDescriptor {
	}
	
	@AsterixVersioned(
			apiMigrations = {},
			objectMapperConfigurer = DummyConfigurer.class,
			version = 1
	)
	@AsterixServiceRegistryApi(exportedApis = GreetingService.class)
	public static class VersionedGreetingApiDescriptor {
	}
	
	public static class DummyConfigurer implements AsterixObjectMapperConfigurer {
		@Override
		public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
		}
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
