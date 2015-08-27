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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.beans.service.FaultToleranceConfigurator.FtProxySetting;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.core.IllegalServiceMetadataException;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.util.ReflectionUtil;

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
	private final BeanFaultToleranceFactory beanFaultToleranceFactory;
	private final DynamicBooleanProperty available;
	
	/*
	 * Guards the state of this service bean instance.
	 */
	private final Lock beanStateLock = new ReentrantLock();
	
	private volatile ServiceProperties currentProperties;
	private volatile BeanState currentState;

	private ServiceBeanInstance(ServiceDefinition<T> serviceDefinition, 
								AstrixBeanKey<T> beanKey, 
								ServiceDiscovery serviceDiscovery, 
								ServiceComponentRegistry serviceComponents,
								BeanFaultToleranceFactory beanFaultToleranceFactory,
								DynamicBooleanProperty available) {
		this.serviceDiscovery = serviceDiscovery;
		this.beanFaultToleranceFactory = beanFaultToleranceFactory;
		this.available = available;
		this.serviceDefinition = Objects.requireNonNull(serviceDefinition);
		this.beanKey = Objects.requireNonNull(beanKey);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.currentState = new Unbound();
	}
	
	public static <T> ServiceBeanInstance<T> create(ServiceDefinition<T> serviceDefinition, 
													AstrixBeanKey<T> beanKey, 
													ServiceDiscovery serviceDiscovery, 
													ServiceBeanContext serviceBeanContext) {
		BeanConfiguration beanConfiguration = serviceBeanContext.getBeanConfigurations().getBeanConfiguration(beanKey);
		return new ServiceBeanInstance<T>(serviceDefinition, 
				beanKey, 
				serviceDiscovery, 
				serviceBeanContext.getServiceComponents(), 
				serviceBeanContext.getBeanFaultToleranceFactory(), 
				beanConfiguration.get(AstrixBeanSettings.AVAILABLE));
	}
	
	public void renewLease() {
		beanStateLock.lock();
		try {
			ServiceProperties serviceProperties = serviceDiscovery.run();
			if (serviceHasChanged(serviceProperties)) {
				bind(serviceProperties);
			} else {
				log.debug("Service properties have not changed. No need to bind bean=" + getBeanKey());
			}
		} catch (Exception e) {
			log.warn(String.format("Failed to renew lease for service bean. bean=%s astrixBeanId=%s", getBeanKey(), id), e);
		} finally {
			beanStateLock.unlock();
		}
	}
	
	private boolean serviceHasChanged(ServiceProperties serviceProperties) {
		return !Objects.equals(currentProperties, serviceProperties);
	}
	
	public void bind() {
		beanStateLock.lock();
		try {
			if (isBound()) {
				return;
			}
			ServiceProperties serviceProperties = serviceDiscovery.run();
			if (serviceProperties == null) {
				log.info(String.format(
					"Failed to discover service using %s. bean=%s astrixBeanId=%s", 
						serviceDiscovery.description(), getBeanKey(), id));
				return;
			}
			bind(serviceProperties);
		} catch (Exception e) {
			log.warn(String.format("Failed to bind service bean. bean=%s astrixBeanId=%s", getBeanKey(), id), e);
		} finally {
			beanStateLock.unlock();
		}
	}
	
	/**
	 * Attempts to bind this bean with the latest serviceProperties, or null if serviceLookup
	 * returned null indicating that service is not available.
	 * 
	 * Throws exception if bind attempt fails.
	 */
	private void bind(ServiceProperties serviceProperties) {
		this.currentState.bindTo(serviceProperties);
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
	
	private ServiceComponent getServiceComponent(ServiceProperties serviceProperties) {
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
					throw new ServiceUnavailableException("Service bean was not bound before timeout. bean=" + beanKey);
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

		protected void bindTo(ServiceProperties serviceProperties) {
			if (serviceProperties == null) {
				transitionToUnboundState();
				return;
			}
			String providerSubsystem = serviceProperties.getProperty(ServiceProperties.SUBSYSTEM);
			if (providerSubsystem == null) {
				providerSubsystem = AstrixSettings.SUBSYSTEM_NAME.defaultValue();
			}
			try {
				ServiceComponent serviceComponent = getServiceComponent(serviceProperties);
				if (!serviceComponent.canBindType(beanKey.getBeanType())) {
					throw new UnsupportedTargetTypeException(serviceComponent.getName(), beanKey.getBeanType());
				}
				BoundServiceBeanInstance<T> boundInstance = serviceComponent.bind(serviceDefinition, serviceProperties);
				T faultToleranceProtectedInstance = addFaultTolerance(serviceComponent, boundInstance.get());
				setState(new Bound(boundInstance, faultToleranceProtectedInstance));
				currentProperties = serviceProperties;
			} catch (IllegalServiceMetadataException e) {
				setState(new IllegalServiceMetadataState(e.getMessage()));
			} catch (Exception e) {
				log.warn(String.format("Failed to bind service bean: %s", getBeanKey()), e);
				setState(new Unbound());
			}
		}

		private T addFaultTolerance(ServiceComponent serviceComponent, T instance) {
			CommandSettings faultToleranceSettings = new CommandSettings();
			FtProxySetting ftProxySetting = FtProxySetting.ENABLED;
			if (serviceComponent instanceof FaultToleranceConfigurator) {
				 ftProxySetting = FaultToleranceConfigurator.class.cast(serviceComponent).configure(faultToleranceSettings);
			}
			if (ftProxySetting == FtProxySetting.ENABLED) {
				return beanFaultToleranceFactory.addFaultToleranceProxy(serviceDefinition, instance, faultToleranceSettings);
			} else {
				log.info("Fault tolerance proxy is disabled by ServiceComponent. componentName={}, beanKey={}", 
						serviceComponent.getName(), getBeanKey().toString());
				return instance;
			}
		}

		protected final void transitionToUnboundState() {
			if (currentState.getClass().equals(Unbound.class)) {
				return;
			}
			setState(new Unbound());
		}
		
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
		private final T faultToleranceProtectedInstance;
		
		public Bound(BoundServiceBeanInstance<T> bean, T faultToleranceProtectedInstance) {
			this.serviceBeanInstance = bean;
			this.faultToleranceProtectedInstance = faultToleranceProtectedInstance;
		}

		@Override
		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			return ReflectionUtil.invokeMethod(method, faultToleranceProtectedInstance, args);
		}

		@Override
		protected void releaseInstance() {
			serviceBeanInstance.release();
		}

		@Override
		protected String name() {
			return "Bound";
		}
	}
	
	private class Unbound extends BeanState {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new ServiceUnavailableException("astrixBeanId=" + id + " bean="+ beanKey);
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
		
		private String message;

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
		protected String name() {
			return "IllegalServiceMetadata";
		}
	}

	String getState() {
		return this.currentState.name();
	}
	
	ServiceProperties getCurrentProperties() {
		return currentProperties;
	}

}
