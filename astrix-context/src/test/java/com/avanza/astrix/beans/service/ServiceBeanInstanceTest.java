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
package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.core.BeanProxyFilter;
import com.avanza.astrix.beans.core.BeanProxyNames;
import com.avanza.astrix.beans.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.beans.ft.BeanFaultTolerance;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.IllegalServiceMetadataException;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import rx.Observable;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ServiceBeanInstanceTest {
	
	private AstrixContext astrixContext;

	@AfterEach
	void destroy() {
		if (astrixContext != null) {
			astrixContext.destroy();
		}
	}
	
	@Test
	void addsFtProxyToServiceBean() {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		final AtomicBoolean ftApplied = new AtomicBoolean(false);
		BeanFaultTolerance beanFt = new BeanFaultTolerance() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
				ftApplied.set(true);
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command) throws Throwable {
				ftApplied.set(true);
				return command.call();
			}
		};
		astrixConfigurer.registerStrategy(BeanFaultToleranceFactorySpi.class, (beanKey) -> beanFt);
		astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		
		ftApplied.set(false);
		assertEquals("foo", ping.ping("foo"));
		assertTrue(ftApplied.get(), "Fault tolerance proxy should be applied to service beans");
	}
	
	@Test
	void ftProxyCanBeDisabledByServiceComponent() {
		DisabledFtComponent component = new DisabledFtComponent();
		
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, component.registerAndGetServiceProperties(Ping.class, new PingImpl()));
		
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerPlugin(ServiceComponent.class, component);
		
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		final AtomicBoolean ftApplied = new AtomicBoolean(false);
		BeanFaultTolerance beanFt = new BeanFaultTolerance() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
				ftApplied.set(true);
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command) throws Throwable {
				ftApplied.set(true);
				return command.call();
			}
		};
		astrixConfigurer.registerStrategy(BeanFaultToleranceFactorySpi.class, beanKey -> beanFt);
		astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		
		ftApplied.set(false);
		assertEquals("foo", ping.ping("foo"));
		assertFalse(ftApplied.get(), "Fault tolerance can be disabled by ServiceComponent");
	}
	
	@Test
	void itsPossibleToSetBeanInInactiveStateUsingBeanSettings() {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());

		astrixContext = astrixConfigurer.configure();
		
		final Ping ping = astrixContext.getBean(Ping.class);
		assertEquals("foo", ping.ping("foo"));
		astrixConfigurer.set(AstrixBeanSettings.AVAILABLE, AstrixBeanKey.create(Ping.class), false);
		AstrixTestUtil.assertThrows(() -> ping.ping("foo"), ServiceUnavailableException.class);
	}
	
	@Test
	void waitForBeanReturnsWhenServiceIsBound() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());

		astrixContext = astrixConfigurer.configure();
		
		// Get bean in unbound state
		astrixContext.getBean(Ping.class);
		
		// Register provider
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		// Bean should be bound
		astrixContext.waitForBean(Ping.class, 5000);
	}
	
	@Test
	void whenServiceNotAvailableOnFirstBindAttemptTheServiceBeanShouldReattemptToBindLater() throws Exception {
		String serviceId = DirectComponent.register(Ping.class, new PingImpl());
		
		TestAstrixConfigurer config = new TestAstrixConfigurer();
		config.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		config.set("pingUri", DirectComponent.getServiceUri(serviceId));
		config.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		AstrixContext context = config.configure();
		
		// Unregister to simulate service that is available in config, but provider not available.
		DirectComponent.unregister(serviceId);
		
		final Ping ping = context.getBean(Ping.class);

		assertThrows(ServiceUnavailableException.class, () -> ping.ping("foo"), "Bean should not be bound");

		DirectComponent.register(Ping.class, new PingImpl(), serviceId);

		assertEventually(() -> ping.ping("foo"), equalTo("foo"));
		
	}
	
	@Test
	void boundServiceInstancesShouldBeReleasedWhenContextIsDestroyed() {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixApplicationContext astrixContext = (AstrixApplicationContext) astrixConfigurer.configure();
		
		
		Ping ping = astrixContext.getBean(Ping.class);
		assertEquals("foo", ping.ping("foo"));

		DirectComponent directComponent = (DirectComponent) astrixContext.getInstance(ServiceComponentRegistry.class).getComponent(AstrixServiceComponentNames.DIRECT);
		assertEquals(2, directComponent.getBoundServices().size());
		assertThat("Expected at least one service to be bound after pingBean is bound", directComponent.getBoundServices().size(), greaterThanOrEqualTo(1));
		
		astrixContext.destroy();
		assertEquals(0, directComponent.getBoundServices().size(), "All bound beans should be release when context is destroyed");
	}
	
	
	@Test
	void boundServiceInstancesShouldBeReleasedWhenMovingToUnboundState() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 5);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixApplicationContext astrixContext = (AstrixApplicationContext) astrixConfigurer.configure();
		
		DirectComponent directComponent = (DirectComponent) astrixContext.getInstance(ServiceComponentRegistry.class).getComponent(AstrixServiceComponentNames.DIRECT);

		final Ping ping = astrixContext.getBean(Ping.class);
		ping.ping("foo");
		
		assertEquals(2, directComponent.getBoundServices().size());
		
		serviceRegistry.clear();
		
		assertEventuallyThrows(() -> ping.ping("foo"), any(ServiceUnavailableException.class));
		
		assertEquals(1, directComponent.getBoundServices().size());
	}

	@Test
	void serviceBeanInstanceUsesDefaultSubsystemNameWhenNoSubsystemIsSetInServiceProperties() {
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
	
	@Test
	void exceptionsThrownArePropagatedForServices() {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				throw new MyException();
			}
			
		});
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixContext = astrixConfigurer.configure();
		
		assertThrows(MyException.class, () -> astrixContext.getBean(Ping.class).ping("foo"));
	}
	
	@Test
	void throwsServiceUnavailableExceptionIfBeanNotBoundBeforeTimeout() throws Exception {
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixContext = astrixConfigurer.configure();

		try {
			astrixContext.waitForBean(Ping.class, 1);
			fail("Expected exception when bean can't be bound before timeout");
		} catch (ServiceUnavailableException e) {
			assertThat("Root cause for waitForBean", e.getCause(), CoreMatchers.instanceOf(NoServiceProviderFound.class));
		}
	}
	
	@Test
	void throwsIllegalMetadataExceptionIfServiceIsNotWellFormed() throws Exception {
		
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
	void beanIsConsideredBoundWhenMovedToIllegalMetadataState() throws Exception {
		
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
	
	
	@Test
	void throwsServiceDiscoveryErrorWhenServiceDiscoveryThrowsAnException() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, new FakeComponent());
		astrixContext = astrixConfigurer.configure();

		try {
			astrixContext.waitForBean(Ping.class, 1).ping("foo");
			fail("Expected ServiceDiscoveryError");
		} catch (ServiceDiscoveryError e) {
			assertThat("Root cause for ServiceDiscoveryError", e.getCause(), CoreMatchers.instanceOf(NoServiceProviderFound.class));
		}
	}
	
	@Test
	void throwsNoServiceProviderFoundWhenNoServiceProviderFound() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, new FakeComponent());
		// No serviceUri for Ping in configuration => NoProviderFound
		astrixContext = astrixConfigurer.configure();

		assertThrows(NoServiceProviderFound.class, () -> astrixContext.waitForBean(Ping.class, 1).ping("foo"), "Expected NoServiceProviderFound");
	}
	
	
	@Test
	void throwsServiceBindErrorWhenBindingADiscoveredServiceFails() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProviderUsingConfigLookup.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, new FakeComponent());
		astrixConfigurer.set("pingUri", "direct:-21"); // DirectComponent will not find the associated provider (since it does not exist)
		astrixContext = astrixConfigurer.configure();

		assertThrows(ServiceBindError.class, () -> astrixContext.waitForBean(Ping.class, 1).ping("foo"), "Expected ServiceBindError");
	}
	
	private <T extends Exception> void assertEventuallyThrows(Supplier<?> invocation, Matcher<T> returnValueMatcher) throws InterruptedException {
		new Poller(1000, 1).check(AstrixTestUtil.serviceInvocationException(invocation, returnValueMatcher));
	}
	
	private <T> void assertEventually(Supplier<T> invocation, Matcher<T> returnValueMatcher) throws InterruptedException {
		new Poller(1000, 1).check(AstrixTestUtil.serviceInvocationResult(invocation, returnValueMatcher));		
	}
	
	static class FakeComponent implements ServiceComponent {
		
		@Override
		public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> versioningContext, ServiceProperties serviceProperties) {
			throw new IllegalServiceMetadataException("Illegal metadata");
		}
		
		@Override
		public String getName() {
			return "test";
		}

		@Override
		public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
			return new ServiceProperties();
		}

		@Override
		public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> exportedServiceDefinition) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canBindType(Class<?> type) {
			return true;
		}

		@Override
		public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
		}

		@Override
		public boolean requiresProviderInstance() {
			return false;
		}
	}
	
	
	private static class MyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public interface PingApiProviderUsingConfigLookup {
		@AstrixConfigDiscovery("pingUri")
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	
	
	public static class DisabledFtComponent implements ServiceComponent, BeanProxyFilter {
		
		private final DirectComponent directComponent = new DirectComponent(new ObjectSerializerFactory() {
			@Override
			public AstrixObjectSerializer create(ObjectSerializerDefinition serializerDefinition) {
				return new AstrixObjectSerializer.NoVersioningSupport();
			}
		}, new ReactiveTypeConverterImpl(Collections.emptyList()));
		
		@Override
		public boolean applyBeanProxy(BeanProxy beanProxy) {
			return !BeanProxyNames.FAULT_TOLERANCE.equals(beanProxy.name());
		}
		
		public <T> ServiceProperties registerAndGetServiceProperties(Class<T> bean, T provider) {
			ServiceProperties result = DirectComponent.registerAndGetProperties(bean, provider);
			result.setComponent(getName());
			return result;
		}

		@Override
		public String getName() {
			return "NO_FT_DIRECT_COMPONENT";
		}

		@Override
		public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
			return directComponent.bind(serviceDefinition, serviceProperties);
		}

		@Override
		public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
			ServiceProperties serviceProperties = directComponent.parseServiceProviderUri(serviceProviderUri);
			serviceProperties.setComponent(getName());
			return serviceProperties;
		}

		@Override
		public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> exportedServiceDefinition) {
			return null;
		}

		@Override
		public boolean canBindType(Class<?> type) {
			return true;
		}

		@Override
		public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
		}

		@Override
		public boolean requiresProviderInstance() {
			return false;
		}

	}
	
	public static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}

}
