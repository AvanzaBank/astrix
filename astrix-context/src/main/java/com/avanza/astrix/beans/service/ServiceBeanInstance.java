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

import static java.util.function.Predicate.isEqual;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BeanInvocationDispatcher;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.core.BeanProxyFilter;
import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.core.IllegalServiceMetadataException;
import com.avanza.astrix.core.ServiceUnavailableException;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class ServiceBeanInstance<T> implements StatefulAstrixBean, InvocationHandler {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceBeanInstance.class);

	private static final AtomicInteger nextId = new AtomicInteger(0);
	private final String id = Integer.toString(nextId.incrementAndGet()); // Used for debugging to distinguish between many context's started within same jvm.
	
	private final AstrixBeanKey<T> beanKey;
	private final ServiceComponentRegistry serviceComponents;
	private final ServiceDefinition<T> serviceDefinition;
	/*
	 * Monitor used to signal state changes. (waitForBean)
	 */
	private final Lock boundStateLock = new ReentrantLock();
	private final Condition boundCondition = boundStateLock.newCondition();
	
	
	private final ServiceDiscovery serviceDiscovery;
	private final DynamicBooleanProperty available;
	
	/*
	 * Guards the state of this service bean instance.
	 */
	private final Lock beanStateLock = new ReentrantLock();
	
	private volatile ServiceProviderInstanceProperties currentProperties;
	private volatile BeanState currentState;

	private final List<BeanProxy> beanProxies;
	private final ReactiveTypeConverter reactiveTypeConverter;

	private ServiceBeanInstance(ServiceDefinition<T> serviceDefinition, 
								AstrixBeanKey<T> beanKey, 
								ServiceDiscovery serviceDiscovery, 
								ServiceComponentRegistry serviceComponents,
								ServiceBeanProxies beanProxies,
								ReactiveTypeConverter reactiveTypeConverter, DynamicBooleanProperty available) {
		this.serviceDiscovery = serviceDiscovery;
		this.reactiveTypeConverter = reactiveTypeConverter;
		this.beanProxies = beanProxies.create(beanKey);
		this.available = available;
		this.serviceDefinition = Objects.requireNonNull(serviceDefinition);
		this.beanKey = Objects.requireNonNull(beanKey);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.currentState = new Unbound(ServiceUnavailableException.class, "No bind attempt run yet");
	}
	
	public static <T> ServiceBeanInstance<T> create(ServiceDefinition<T> serviceDefinition, 
													AstrixBeanKey<T> beanKey, 
													ServiceDiscovery serviceDiscovery, 
													ServiceBeanContext serviceBeanContext) {
		BeanConfiguration beanConfiguration = serviceBeanContext.getConfig().getBeanConfiguration(beanKey);
		return new ServiceBeanInstance<T>(serviceDefinition, 
				beanKey, 
				serviceDiscovery, 
				serviceBeanContext.getServiceComponents(), 
				serviceBeanContext.getServiceBeanProxies(),
				serviceBeanContext.getReactiveTypeConverter(),
				beanConfiguration.get(AstrixBeanSettings.AVAILABLE));
	}
	
	public void renewLease() {
		beanStateLock.lock();
		try {
			ServiceDiscoveryResult serviceDiscoveryResult = runServiceDiscovery();
			if (!serviceDiscoveryResult.isSuccessful()) {
				log.warn(String.format("Failed to renew lease, service discovery failure. bean=%s astrixBeanId=%s", getBeanKey(), id), serviceDiscoveryResult.getError());
				return;
			}
			if (serviceHasChanged(serviceDiscoveryResult.getResult())) {
				bind(serviceDiscoveryResult.getResult());
			} else {
				log.debug("Service properties have not changed. No need to bind bean=" + getBeanKey());
			}
		} catch (Exception e) {
			log.warn(String.format("Failed to renew lease for service bean. bean=%s astrixBeanId=%s", getBeanKey(), id), e);
		} finally {
			beanStateLock.unlock();
		}
	}
	
	private boolean serviceHasChanged(ServiceProviders serviceProviders) {
		return !serviceProviders.getInstanceProperties().stream()
				.filter(isEqual(currentProperties))
				.findAny()
				.isPresent();
	}
	
	public void bind() {
		beanStateLock.lock();
		try {
			if (isBound()) {
				return;
			}
			ServiceDiscoveryResult serviceDiscoveryResult = runServiceDiscovery();
			if (!serviceDiscoveryResult.isSuccessful()) {
				log.warn(String.format("Service discovery failure. bean=%s astrixBeanId=%s", getBeanKey(), id), serviceDiscoveryResult.getError());
				currentState.setState(new Unbound(ServiceDiscoveryError.class, "An error occured during last service discovery attempt, see cause for details.", serviceDiscoveryResult.getError()));
				return;
				
			}
			if (serviceDiscoveryResult.getResult().isEmpty()) {
				log.info(String.format(
					"Did not discover a service provider using %s. bean=%s astrixBeanId=%s", 
						serviceDiscovery.description(), getBeanKey(), id));
				currentState.setState(new Unbound(NoServiceProviderFound.class, "Did not discover a service provider for " + getBeanKey().getBeanType().getSimpleName() + " on last service discovery attempt. discoveryStrategy=" + serviceDiscovery.description()));
				return;
			}
			bind(serviceDiscoveryResult.getResult());
		} catch (Exception e) {
			log.warn(String.format("Failed to bind service bean. bean=%s astrixBeanId=%s", getBeanKey(), id), e);
		} finally {
			beanStateLock.unlock();
		}
	}
	
	private ServiceDiscoveryResult runServiceDiscovery() {
		try {
			return ServiceDiscoveryResult.successful(serviceDiscovery.run());
		} catch (Exception e) {
			return ServiceDiscoveryResult.failure(e);
		}
	}
	
	static class ServiceDiscoveryResult {
		final ServiceProviders serviceProviders;
		final Exception discoveryError;
		
		ServiceDiscoveryResult(ServiceProviders serviceProviders, Exception discoveryError) {
			this.serviceProviders = serviceProviders;
			this.discoveryError = discoveryError;
		}

		static ServiceDiscoveryResult failure(Exception e) {
			return new ServiceDiscoveryResult(new ServiceProviders(new ArrayList<>()), e);
		}
		
		static ServiceDiscoveryResult successful(ServiceProviders serviceProviders) {
			return new ServiceDiscoveryResult(serviceProviders, null);
		}
		
		boolean isSuccessful() {
			return discoveryError == null;
		}
		
		public ServiceProviders getResult() {
			return serviceProviders;
		}
		
		public Exception getError() {
			return discoveryError;
		}
		
	}
	
	
	/**
	 * Attempts to bind this bean with the latest serviceProperties, or null if serviceLookup
	 * returned null indicating that service is not available.
	 * 
	 * Throws exception if bind attempt fails.
	 */
	private void bind(ServiceProviders serviceProperties) {
		this.currentState.bindTo(serviceProperties.getRandom());
	}
	
	void destroy() {
		log.info("Destroying service bean. bean={} astrixBeanId={}", getBeanKey(), id);
		beanStateLock.lock();
		try {
			this.currentState.releaseInstance();
		} finally {
			beanStateLock.unlock();
		}
	}

	private void notifyBound() {
		boundStateLock.lock();
		try {
			boundCondition.signalAll();
		} finally {
			boundStateLock.unlock();
		}
	}
	
	private ServiceComponent getServiceComponent(ServiceProviderInstanceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("Expected a componentName to be set on serviceProperties: " + serviceProperties);
		}
		return serviceComponents.getComponent(componentName);
	}
	@Override
	public void waitUntilBound(long timeoutMillis) throws InterruptedException {
		boundStateLock.lock();
		try {
			if (!isBound()) {
				if (!boundCondition.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
					this.currentState.verifyBound(); // Let current state throw exception describing cause
				}
			}
		} finally {
			boundStateLock.unlock();
		}
	}
	
	/**
	 * A bean is considered bound if it is in any state except Unbound.
	 *  
	 * @return
	 */
	public boolean isBound() {
		return !this.currentState.getClass().equals(Unbound.class);
	}
	
	public AstrixBeanKey<T> getBeanKey() {
		return beanKey;
	}
	
	String getBeanId() {
		return this.id;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		if (method.getDeclaringClass().equals(StatefulAstrixBean.class)) {
			try {
				return method.invoke(this, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
		if (method.getDeclaringClass().equals(Object.class)) {
			return method.invoke(this, args);
		}
		if (!this.available.get()) {
			throw new ServiceUnavailableException("Service is explicitly set in unavailable state");
		}
		return this.currentState.invoke(proxy, method, args);
	}
	
	private abstract class BeanState implements InvocationHandler {

		protected void bindTo(ServiceProviderInstanceProperties serviceProperties) {
			if (serviceProperties == null) {
				setState(new Unbound(NoServiceProviderFound.class, "No service provider found"));
				return;
			}
			String providerSubsystem = serviceProperties.getProperty(ServiceProviderInstanceProperties.SUBSYSTEM);
			if (providerSubsystem == null) {
				providerSubsystem = AstrixSettings.SUBSYSTEM_NAME.defaultValue();
			}
			try {
				ServiceComponent serviceComponent = getServiceComponent(serviceProperties);
				if (!serviceComponent.canBindType(beanKey.getBeanType())) {
					throw new UnsupportedTargetTypeException(serviceComponent.getName(), beanKey.getBeanType());
				}
				BoundServiceBeanInstance<T> boundInstance = serviceComponent.bind(serviceDefinition, serviceProperties);
				BeanInvocationDispatcher beanInvocationDispatcher = new BeanInvocationDispatcher(getBeanProxies(serviceComponent)
																								,reactiveTypeConverter, 
																								boundInstance.get());
				setState(new Bound(boundInstance, beanInvocationDispatcher));
				currentProperties = serviceProperties;
			} catch (IllegalServiceMetadataException e) {
				setState(new IllegalServiceMetadataState(e.getMessage()));
			} catch (Exception e) {
				log.warn(String.format("Failed to bind service bean: %s", getBeanKey()), e);
				setState(new Unbound(ServiceBindError.class, "Failed to bind " + getBeanKey().getBeanType().getSimpleName() + " using serviceProperties=" + serviceProperties +  ", see cause for details.", e));
			}
		}

		private List<BeanProxy> getBeanProxies(ServiceComponent serviceComponent) {
			if (!(serviceComponent instanceof BeanProxyFilter)) {
				return beanProxies;
			}
			BeanProxyFilter filter = BeanProxyFilter.class.cast(serviceComponent);
			return beanProxies.stream()
							  .filter(beanProxy -> {
								  boolean applyBeanProxy = filter.applyBeanProxy(beanProxy);
								  if (!applyBeanProxy) {
									  log.info("BeanProxy is disabled by ServiceComponent. beanProxy={} componentName={} beanKey={}", 
											  beanProxy.name(), serviceComponent.getName(), serviceDefinition.getBeanKey().toString());
								  }
								  return applyBeanProxy;
							  })
							  .collect(Collectors.toList());
		}

		protected abstract void verifyBound();

		protected final void setState(BeanState newState) {
			if (!currentState.getClass().equals(newState.getClass())) {
				log.info(String.format("Service bean entering new state. newState=%s bean=%s id=%s", newState.name(), beanKey, id));
			}
			currentState = newState;
			if (isBoundState(newState)) {
				notifyBound();
			}
			releaseInstance();
		}

		private boolean isBoundState(BeanState newState) {
			return !newState.getClass().equals(Unbound.class);
		}

		protected abstract String name();
		
		protected abstract void releaseInstance();
		
	}
	
	private class Bound extends BeanState {

		private final BoundServiceBeanInstance<T> serviceBeanInstance;
		private final BeanInvocationDispatcher serviceBeanInvocationDispatcher;
		
		public Bound(BoundServiceBeanInstance<T> bean, BeanInvocationDispatcher serviceBeanInvocationDispatcher) {
			this.serviceBeanInstance = bean;
			this.serviceBeanInvocationDispatcher = serviceBeanInvocationDispatcher;
		}

		@Override
		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			return serviceBeanInvocationDispatcher.invoke(proxy, method, args);
		}

		@Override
		protected void releaseInstance() {
			serviceBeanInstance.release();
		}

		@Override
		protected String name() {
			return "Bound";
		}

		@Override
		protected void verifyBound() {
		}
	}
	
	private class Unbound extends BeanState {
		
		private final Class<? extends ServiceUnavailableException> exceptionFactory;
		private final String message;
		private final Exception discoveryFailure;
		
		public Unbound(Class<? extends ServiceUnavailableException> exceptionFactory, String message) {
			this(exceptionFactory, message, null);
		}
		
		public Unbound(Class<? extends ServiceUnavailableException> exceptionFactory, String message, Exception discoveryFailureCause) {
			this.exceptionFactory = exceptionFactory;
			this.discoveryFailure = discoveryFailureCause;
			this.message = message;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw createServiceUnavailableException();
		}

		private ServiceUnavailableException createServiceUnavailableException() {
			ServiceUnavailableException exception = initException();
			if (discoveryFailure != null) {
				exception.initCause(discoveryFailure);
			}
			return exception;
		}

		private ServiceUnavailableException initException() {
			try {
				return exceptionFactory.getConstructor(String.class).newInstance(message + " astrixBeanId=" + id + " bean=" + beanKey);
			} catch (Exception e) {
				return new ServiceUnavailableException(message);
			}
		}
		
		@Override
		protected void verifyBound() {
			throw createServiceUnavailableException();
		}
		
		@Override
		protected void releaseInstance() {
		}
		@Override
		protected String name() {
			return "Unbound";
		}
	}
	
	private class IllegalServiceMetadataState extends BeanState {
		
		private final String message;

		public IllegalServiceMetadataState(String message) {
			this.message = message;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new IllegalServiceMetadataException(String.format("bean=%s astrixBeanId=%s message=%s", beanKey, id, message));
		}
		
		@Override
		protected void releaseInstance() {
		}
		
		@Override
		protected void verifyBound() {
			throw new IllegalServiceMetadataException(String.format("bean=%s astrixBeanId=%s message=%s", beanKey, id, message));
		}
		
		@Override
		protected String name() {
			return "IllegalServiceMetadata";
		}
	}

	String getState() {
		return this.currentState.name();
	}
	
	ServiceProviderInstanceProperties getCurrentProperties() {
		return currentProperties;
	}

}
