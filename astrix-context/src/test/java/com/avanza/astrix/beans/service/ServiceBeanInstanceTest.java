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

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.beans.ft.FaultToleranceConfigurator;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.context.core.ReactiveTypeConverterImpl;
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

import rx.Observable;

public class ServiceBeanInstanceTest {
	
	private AstrixContext astrixContext;

	@After
	public void destroy() {
		if (astrixContext != null) {
			astrixContext.destroy();
		}
	}
	
	@Test
	public void addsFtProxyToServiceBean() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		final AtomicBoolean ftApplied = new AtomicBoolean(false);
		astrixConfigurer.registerStrategy(FaultToleranceSpi.class, new FaultToleranceSpi() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable, CommandSettings settings) {
				ftApplied.set(true);
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command, CommandSettings settings) throws Throwable {
				ftApplied.set(true);
				return command.call();
			}
		});
		astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		
		ftApplied.set(false);
		assertEquals("foo", ping.ping("foo"));
		assertTrue("Fault tolerance proxy should be applied to service beans", ftApplied.get());
	}
	
	@Test
	public void ftProxyCanBeDisabledByServiceComponent() throws Exception {
		DisabledFtComponent component = new DisabledFtComponent();
		
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, component.registerAndGetServiceProperties(Ping.class, new PingImpl()));
		
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerPlugin(ServiceComponent.class, component);
		
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		final AtomicBoolean ftApplied = new AtomicBoolean(false);
		astrixConfigurer.registerStrategy(FaultToleranceSpi.class, new FaultToleranceSpi() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable, CommandSettings settings) {
				ftApplied.set(true);
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command, CommandSettings settings) throws Throwable {
				ftApplied.set(true);
				return command.call();
			}
		});
		astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		
		ftApplied.set(false);
		assertEquals("foo", ping.ping("foo"));
		assertFalse("Fault tolerance can be disabled by ServiceComponent", ftApplied.get());
	}
	
	@Test
	public void itsPossibleToSetBeanInInactiveStateUsingBeanSettings() throws Exception {
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
	public void waitForBeanReturnsWhenServiceIsBound() throws Exception {
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
	public void whenServiceNotAvailableOnFirstBindAttemptTheServiceBeanShouldReattemptToBindLater() throws Exception {
		String serviceId = DirectComponent.register(Ping.class, new PingImpl());
		
		TestAstrixConfigurer config = new TestAstrixConfigurer();
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

		assertEventually(() -> ping.ping("foo"), equalTo("foo"));
		
	}
	
	@Test
	public void boundServiceInstancesShouldBeReleasedWhenContextIsDestroyed() throws Exception {
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
		assertEquals("All bound beans should be release when context is destroyed", 0, directComponent.getBoundServices().size());
	}
	
	
	@Test
	public void boundServiceInstancesShouldBeReleasedWhenMovingToUnboundState() throws Exception {
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
			public String ping(String msg) {
				throw new MyException();
			}
			
		});
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixContext = astrixConfigurer.configure();
		
		astrixContext.getBean(Ping.class).ping("foo");
	}
	
	@Test
	public void throwsServiceUnavailableExceptionIfBeanNotBoundBeforeTimeout() throws Exception {
		
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
	
	
	static class MyException extends RuntimeException {
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
	
	
	
	public static class DisabledFtComponent implements ServiceComponent, FaultToleranceConfigurator {
		
		private final DirectComponent directComponent = new DirectComponent(new ObjectSerializerFactory() {
			@Override
			public AstrixObjectSerializer create(ObjectSerializerDefinition serializerDefinition) {
				return new AstrixObjectSerializer.NoVersioningSupport();
			}
		}, new ReactiveTypeConverterImpl(Collections.emptyList()));
		
		@Override
		public FtProxySetting configure() {
			return FtProxySetting.DISABLED;
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
