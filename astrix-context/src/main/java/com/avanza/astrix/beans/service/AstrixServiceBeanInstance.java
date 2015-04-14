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

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixServiceBeanInstance<T> implements StatefulAstrixBean, InvocationHandler {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixServiceBeanInstance.class);

	private static final AtomicInteger nextId = new AtomicInteger(0);
	private final String id = Integer.toString(nextId.incrementAndGet()); // Used for debugging to distinguish between many context's started within same jvm.
	
	private final AstrixBeanKey<T> beanKey;
	private final AstrixServiceComponents serviceComponents;
	private final String subsystem;
	private final DynamicBooleanProperty enforceSubsystemBoundaries;
	private final ServiceVersioningContext versioningContext;
	/*
	 * Monitor used to signal state changes. (waitForBean)
	 */
	private final Lock boundStateLock = new ReentrantLock();
	private final Condition boundCondition = boundStateLock.newCondition();
	
	
	private final AstrixServiceLookup serviceLookup;
	
	/*
	 * Guards the state of this service bean instance.
	 */
	private final Lock beanStateLock = new ReentrantLock();
	
	private volatile AstrixServiceProperties currentProperties;
	private volatile BeanState currentState;

	private AstrixServiceBeanInstance(ServiceVersioningContext versioningContext, 
								AstrixBeanKey<T> beanKey, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								DynamicConfig config) {
		this.serviceLookup = serviceLookup;
		this.versioningContext = Objects.requireNonNull(versioningContext);
		this.beanKey = Objects.requireNonNull(beanKey);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.subsystem = config.getStringProperty(AstrixSettings.SUBSYSTEM_NAME, AstrixSettings.DEFAULT_SUBSYSTEM_NAME).get();
		this.enforceSubsystemBoundaries = config.getBooleanProperty(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
		this.currentState = new Unbound();
		log.info(String.format("Start managing service bean. currentState=%s bean=%s astrixBeanId=%s", currentState.name(), beanKey, id));
	}
	
	public static <T> AstrixServiceBeanInstance<T> create(ServiceVersioningContext versioningContext, 
								AstrixBeanKey<T> beanKey, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								DynamicConfig config) {
		return new AstrixServiceBeanInstance<T>(versioningContext, beanKey, serviceLookup, serviceComponents, config);
	}
	
	public void renewLease() {
		beanStateLock.lock();
		try {
			AstrixServiceProperties serviceProperties = serviceLookup.lookup(getBeanKey());
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
	
	private boolean serviceHasChanged(AstrixServiceProperties serviceProperties) {
		return !Objects.equals(currentProperties, serviceProperties);
	}
	
	public void bind() {
		beanStateLock.lock();
		try {
			if (isBound()) {
				return;
			}
			AstrixServiceProperties serviceProperties = serviceLookup.lookup(getBeanKey());
			if (serviceProperties == null) {
				log.info(String.format(
					"Failed to discover service bean. ServiceLookup returned null. bean=%s astrixBeanId=%s", 
						getBeanKey(), id));
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
	private void bind(AstrixServiceProperties serviceProperties) {
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
	
	
	private boolean isAllowedToInvokeService(String providerSubsystem) {
		if (versioningContext.isVersioned()) {
			return true;
		}
		if (!enforceSubsystemBoundaries.get()) {
			return true;
		}
		return this.subsystem.equals(providerSubsystem);
	}
	
	private AstrixServiceComponent getServiceComponent(AstrixServiceProperties serviceProperties) {
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
		return this.currentState.invoke(proxy, method, args);
	}
	
	private abstract class BeanState implements InvocationHandler {

		protected void bindTo(AstrixServiceProperties serviceProperties) {
			if (serviceProperties == null) {
				transitionToUnboundState();
				return;
			}
			String providerSubsystem = serviceProperties.getProperty(AstrixServiceProperties.SUBSYSTEM);
			if (providerSubsystem == null) {
				providerSubsystem = AstrixSettings.DEFAULT_SUBSYSTEM_NAME;
			}
			if (!isAllowedToInvokeService(providerSubsystem)) {
				setState(new IllegalSubsystemState(subsystem, providerSubsystem, beanKey.getBeanType()));
				return;
			} 
			try {
				AstrixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
				if (!serviceComponent.canBindType(beanKey.getBeanType())) {
					throw new UnsupportedTargetTypeException(serviceComponent.getName(), beanKey.getBeanType());
				}
				BoundServiceBeanInstance<T> boundInstance = serviceComponent.bind(versioningContext, beanKey.getBeanType(), serviceProperties);
				setState(new Bound(boundInstance));
				currentProperties = serviceProperties;
			} catch (IllegalServiceMetadataException e) {
				setState(new IllegalServiceMetadataState(e.getMessage()));
			} catch (Exception e) {
				log.warn(String.format("Failed to bind service bean: %s", getBeanKey()), e);
				setState(new Unbound());
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
	
	private class IllegalSubsystemState extends BeanState {
		
		private String currentSubsystem;
		private String providerSubsystem;
		private Class<?> beanType;

		public IllegalSubsystemState(String currentSubsystem, String providerSubsystem, Class<?> beanType) {
			this.currentSubsystem = currentSubsystem;
			this.providerSubsystem = providerSubsystem;
			this.beanType = beanType;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new IllegalSubsystemException(currentSubsystem, providerSubsystem, beanType);
		}
		
		@Override
		protected void releaseInstance() {
		}
		
		@Override
		protected String name() {
			return "IllegalSubsystemState";
		}
		
	}
	
	private class Bound extends BeanState {

		private final BoundServiceBeanInstance<T> serviceBeanInstance;
		
		public Bound(BoundServiceBeanInstance<T> bean) {
			this.serviceBeanInstance = bean;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				return method.invoke(serviceBeanInstance.get(), args);
			} catch (InvocationTargetException e) {
				log.debug("Service invocation threw exception", e);
				throw e.getTargetException();
			}
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
}
