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

import static com.avanza.asterix.test.util.AsterixTestUtil.serviceInvocationResult;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import com.avanza.asterix.context.AsterixContext;
import com.avanza.asterix.context.AsterixDirectComponent;
import com.avanza.asterix.context.AsterixSettings;
import com.avanza.asterix.context.TestAsterixConfigurer;
import com.avanza.asterix.core.ServiceUnavailableException;
import com.avanza.asterix.provider.core.AsterixServiceRegistryApi;
import com.avanza.asterix.service.registry.client.AsterixServiceRegistry;
import com.avanza.asterix.service.registry.client.AsterixServiceRegistryClient;
import com.avanza.asterix.service.registry.client.AsterixServiceRegistryLibrary;
import com.avanza.asterix.service.registry.server.AsterixServiceRegistryEntry;
import com.avanza.asterix.service.registry.util.InMemoryServiceRegistry;
import com.avanza.asterix.test.util.Poller;
import com.avanza.asterix.test.util.Probe;
import com.avanza.asterix.test.util.Supplier;



public class AsterixServiceRegistryLeaseManagerTest {

	private AsterixServiceRegistryClient serviceRegistryClient;
	private AsterixContext context;
	private CorruptableServiceRegistry serviceRegistry;
	private TestService testService;
	
	@Before
	public void setup() {
		serviceRegistry = new CorruptableServiceRegistry();
		TestAsterixConfigurer asterixConfig = new TestAsterixConfigurer();
		asterixConfig.set(AsterixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 1); // No Sleep between attempts
		asterixConfig.set(AsterixSettings.BEAN_REBIND_ATTEMPT_INTERVAL, 1);
		asterixConfig.registerApiDescriptor(TestDescriptor.class);
		asterixConfig.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		asterixConfig.registerApi(AsterixServiceRegistry.class, serviceRegistry);
		context = asterixConfig.configure();
		serviceRegistryClient = context.getBean(AsterixServiceRegistryClient.class);
		
		TestService impl = new TestService() {
			@Override
			public String call() {
				return "1";
			}
		};
		String id = AsterixDirectComponent.register(TestService.class, impl);
		serviceRegistryClient.register(TestService.class, AsterixDirectComponent.getServiceProperties(id), -1);

		testService = context.getBean(TestService.class);
	}
	
	@Test
	public void leaseManagerShouldRecoverFromFailuresInCommunicationWithServiceRegistry() throws Exception {
		// Verify that we are bound to an instance
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return testService.call();
			}
		}, CoreMatchers.equalTo("1")));
		
		// Simulate failure in service registry invocation
		serviceRegistry.exceptionsToThrow = new CountDownLatch(2);
		boolean exceptionsThrown = serviceRegistry.exceptionsToThrow.await(1, TimeUnit.SECONDS);
		if (!exceptionsThrown) {
			fail("Expected lease manager to invoke service registry at least two times");
		}
		
		// Service registry available again. Register new provider for 
		// TestService and verify that client is rebound to new provider 
		TestService impl = new TestService() {
			@Override
			public String call() {
				return "2";
			}
		};
		String id = AsterixDirectComponent.register(TestService.class, impl);
		serviceRegistryClient.register(TestService.class, AsterixDirectComponent.getServiceProperties(id), -1);
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return testService.call();
			}
		}, CoreMatchers.equalTo("2")));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(1000, 1).check(probe);
	}
	
	public static class CorruptableServiceRegistry extends InMemoryServiceRegistry {
		private volatile CountDownLatch exceptionsToThrow = new CountDownLatch(0);
		
		@Override
		public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
			throwIfCorrupt();
			return super.lookup(type, qualifier);
		}
		
		@Override
		public <T> void register(AsterixServiceRegistryEntry properties, long lease) {
			throwIfCorrupt();
			super.register(properties, lease);
		}
		

		private void throwIfCorrupt() {
			if (exceptionsToThrow.getCount() > 0) {
				exceptionsToThrow.countDown();
				throw new ServiceUnavailableException("Registry not avaialable"); 
			}
		}
		
	}
	
	@AsterixServiceRegistryApi(
		exportedApis = {
			TestService.class
		}
	)
	public static class TestDescriptor {
	}
	
	public interface TestService {
		String call();
	}

}
