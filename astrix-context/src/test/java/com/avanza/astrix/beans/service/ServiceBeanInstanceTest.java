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
package com.avanza.astrix.beans.service;

import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationException;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryServiceProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.DirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.IllegalServiceMetadataException;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.test.util.Supplier;

public class ServiceBeanInstanceTest {
	
	private AstrixContext astrixContext;

	@After
	public void destroy() {
		if (astrixContext != null) {
			astrixContext.destroy();
		}
	}
	
	@Test
	public void waitForBeanReturnsWhenServiceIsBound() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());

		astrixContext = astrixConfigurer.configure();
		
		// Get bean in unbound state
		astrixContext.getBean(Ping.class);
		
		// Register provider
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		// Bean should be bound
		astrixContext.waitForBean(Ping.class, 100);
	}
	
	@Test
	public void whenServiceNotAvailableOnFirstBindAttemptTheServiceBeanShouldReattemptToBindLater() throws Exception {
		String serviceId = DirectComponent.register(Ping.class, new PingImpl());
		
		TestAstrixConfigurer config = new TestAstrixConfigurer();
		config.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		config.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		config.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		config.set("pingUri", DirectComponent.getServiceUri(serviceId));
		config.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		AstrixContext context = config.configure();
		
		// Unregister to simulate service that is available in config, but provider not available.
		DirectComponent.unregister(serviceId);
		
		final Ping ping = context.getBean(Ping.class);
		try {
			ping.ping("foo");
			fail("Bean should not be bound");
		} catch (ServiceUnavailableException e) {
			// expected
		}
		
		DirectComponent.register(Ping.class, new PingImpl(), serviceId);

		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() throws Exception {
				return ping.ping("foo");
			}
		}, equalTo("foo")));
		
	}
	
	@Test
	public void boundServiceInstancesShouldBeReleasedWhenContextIsDestroyed() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixApplicationContext astrixContext = (AstrixApplicationContext) astrixConfigurer.configure();
		
		
		Ping ping = astrixContext.getBean(Ping.class);
		assertEquals("foo", ping.ping("foo"));

		DirectComponent directComponent = astrixContext.getInstance(ServiceComponents.class).getComponent(DirectComponent.class);
		assertEquals(2, directComponent.getBoundServices().size());
		assertThat("Expected at least one service to be bound after pingBean is bound", directComponent.getBoundServices().size(), greaterThanOrEqualTo(1));
		
		astrixContext.destroy();
		assertEquals("All bound beans should be release when context is destroyed", 0, directComponent.getBoundServices().size());
	}
	
	
	@Test
	public void boundServiceInstancesShouldBeReleasedWhenMovingToUnboundState() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 5);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixApplicationContext astrixContext = (AstrixApplicationContext) astrixConfigurer.configure();
		
		DirectComponent directComponent = astrixContext.getInstance(ServiceComponents.class).getComponent(DirectComponent.class);

		final Ping ping = astrixContext.getBean(Ping.class);
		ping.ping("foo");
		
		assertEquals(2, directComponent.getBoundServices().size());
		
		serviceRegistry.clear();
		assertEventually(serviceInvocationException(new Supplier<String>() {
			@Override
			public String get() throws Exception {
				return ping.ping("foo");
			}
		}, CoreMatchers.any(ServiceUnavailableException.class)));
		
		assertEquals(1, directComponent.getBoundServices().size());
	}
	
	
	private void assertEventually(Probe serviceInvocationException)
			throws InterruptedException {
		new Poller(100, 1).check(serviceInvocationException);
	}
	
	@Test
	public void serviceBeanInstanceUsesDefaultSubsystemNameWhenNoSubsystemIsSetInServiceProperties() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		String serviceUri = DirectComponent.registerAndGetUri(Ping.class, new PingImpl());
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		astrixConfigurer.setSubsystem("default");
		astrixConfigurer.set("pingUri", serviceUri);
		astrixConfigurer.set(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixContext astrixContext = astrixConfigurer.configure();
		
		assertEquals("foo", astrixContext.getBean(Ping.class).ping("foo"));
	}
	
	@Test(expected = MyException.class)
	public void exceptionsThrownArePropagatedForServices() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new Ping() {
			@Override
			public String ping(String msg) throws MyException {
				throw new MyException();
			}
			
		});
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixContext = astrixConfigurer.configure();
		
		astrixContext.getBean(Ping.class).ping("foo");
	}
	
	@Test
	public void throwsServiceUnavailableExceptionIfBeanNotBoundBeforeTimeout() throws Exception {
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixContext = astrixConfigurer.configure();

		try {
			astrixContext.waitForBean(Ping.class, 1);
			fail("Expected exception when bean can't be bound before timeout");
		} catch (ServiceUnavailableException e) {
			assertThat("Expected ServiceUnavailableException when bean timeout occurs:", e.getMessage().toLowerCase(), CoreMatchers.containsString("bean was not bound before timeout"));
		}
	}
	
	@Test
	public void throwsIllegalMetadataExceptionIfServiceIsNotWellFormed() throws Exception {
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, new FakeComponent());
		astrixConfigurer.set("pingUri", "test:");
		astrixContext = astrixConfigurer.configure();

		try {
			astrixContext.waitForBean(Ping.class, 1).ping("foo");
		} catch (IllegalServiceMetadataException e) {
			assertThat("Expected IllegalServiceMetadataException with message: ", e.getMessage().toLowerCase(), CoreMatchers.containsString("illegal metadata"));
		}
	}
	
	@Test
	public void beanIsConsideredBoundWhenMovedToIllegalMetadataState() throws Exception {
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1);
		astrixConfigurer.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, new FakeComponent());
		AstrixContext astrixContext = astrixConfigurer.configure();

		// Get bean, will be unbound
		astrixContext.getBean(Ping.class);
		
		// Register provider with illegal metadata
		astrixConfigurer.set("pingUri", "test:");
		
		astrixContext.waitForBean(Ping.class, 100);
	}
	
	static class FakeComponent extends DirectComponent {
		
		@Override
		public <T> BoundServiceBeanInstance<T> bind(Class<T> type, ServiceContext versioningContext, ServiceProperties serviceProperties) {
			throw new IllegalServiceMetadataException("Illegal metadata");
		}
		
		@Override
		public String getName() {
			return "test";
		}
	}
	
	
	static class MyException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public interface PingApiProviderUsingConfigLookup {
		@AstrixConfigLookup("pingUri")
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping(String msg) throws Exception;
	}
	
	public static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}

}
