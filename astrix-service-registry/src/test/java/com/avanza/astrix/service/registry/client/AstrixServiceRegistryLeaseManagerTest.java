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

import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryEntry;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.DirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;



public class AstrixServiceRegistryLeaseManagerTest {

	private ServiceRegistryExporterClient serviceRegistryExporterClient;
	private AstrixContext context;
	private CorruptableServiceRegistry serviceRegistry = new CorruptableServiceRegistry();
	private TestService testService;
	
	@Before
	public void setup() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 100); // No Sleep between attempts
		astrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(TestProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerAstrixBean(AstrixServiceRegistry.class, serviceRegistry);
		context = astrixConfigurer.configure();
		serviceRegistryExporterClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "bar");
		
		TestService impl = new TestService() {
			@Override
			public String call() {
				return "1";
			}
		};
		String id = DirectComponent.register(TestService.class, impl);
		serviceRegistryExporterClient.register(TestService.class, DirectComponent.getServiceProperties(id), -1);

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
			fail("Expected lease manager to invoke service registry at least two times: " + serviceRegistry.exceptionsToThrow.getCount());
		}
		
		// Service registry available again. Register new provider for 
		// TestService and verify that client binds to new provider 
		TestService impl = new TestService() {
			@Override
			public String call() {
				return "2";
			}
		};
		String id = DirectComponent.register(TestService.class, impl);
		serviceRegistryExporterClient.register(TestService.class, DirectComponent.getServiceProperties(id), -1);
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
		public <T> AstrixServiceRegistryEntry lookup(String type, String qualifier, ServiceConsumerProperties serviceConsumerProperties) {
			throwIfCorrupt();
			return super.lookup(type, qualifier, serviceConsumerProperties);
		}
		
		@Override
		public List<AstrixServiceRegistryEntry> listServices(String type,
				String qualifier) {
			throwIfCorrupt();
			return super.listServices(type, qualifier);
		}
		
		@Override
		public <T> void register(AstrixServiceRegistryEntry properties, long lease) {
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
	
	@AstrixApiProvider
	interface TestProvider {
		
		@Service
		TestService testService(); 
	}
	
	public interface TestService {
		String call();
	}

}
