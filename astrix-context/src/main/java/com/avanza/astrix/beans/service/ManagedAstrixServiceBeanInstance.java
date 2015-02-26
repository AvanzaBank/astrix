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
public class ManagedAstrixServiceBeanInstance<T> implements StatefulAstrixBean, InvocationHandler {
	
	private static final Logger log = LoggerFactory.getLogger(ManagedAstrixServiceBeanInstance.class);

	private static final AtomicInteger nextId = new AtomicInteger(0);
	private final String id = nextId.incrementAndGet() + ""; // Used for debugging to distinguish between many context's started within same jvm.
	
	private final AstrixBeanKey<T> beanKey;
	private final AstrixServiceComponents serviceComponents;
	private final String subsystem;
	private final DynamicBooleanProperty enforceSubsystemBoundaries;
	private final ServiceVersioningContext versioningContext;
	private volatile InvocationHandler state;

	private final Lock boundStateLock = new ReentrantLock();
	private final Condition boundCondition = boundStateLock.newCondition();

	private ManagedAstrixServiceBeanInstance(ServiceVersioningContext versioningContext, 
								AstrixBeanKey<T> beanKey, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								DynamicConfig config) {
		this.versioningContext = Objects.requireNonNull(versioningContext);
		this.beanKey = Objects.requireNonNull(beanKey);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.subsystem = Objects.requireNonNull(config.getStringProperty(AstrixSettings.SUBSYSTEM_NAME, null).get());
		this.enforceSubsystemBoundaries = config.getBooleanProperty(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
		this.state = new Unbound();
	}
	
	public static <T> ManagedAstrixServiceBeanInstance<T> create(ServiceVersioningContext versioningContext, 
								AstrixBeanKey<T> beanKey, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								DynamicConfig config) {
		return new ManagedAstrixServiceBeanInstance<T>(versioningContext, beanKey, serviceLookup, serviceComponents, config);
	}
	
	/**
	 * Attempts to bind this bean.
	 * 
	 * Throws exception if bind attempt fails.
	 */
	public void bind(AstrixServiceProperties serviceProperties) {
		InvocationHandler previousState = this.state;
		if (serviceProperties == null) {
			log.info("Service moving to unbound state " + beanKey + ", AstrixBeanId=" + id);
			this.state = new Unbound();
			releaseInstanceIfBound(previousState);
			return;
		}
		String providerSubsystem = serviceProperties.getProperty(AstrixServiceProperties.SUBSYSTEM);
		if (!isAllowedToInvokeService(providerSubsystem)) {
			this.state = new IllegalSubsystemState(subsystem, providerSubsystem, beanKey.getBeanType());
		} else {
			this.state = create(serviceProperties);
			log.info("Successfully bound to " + beanKey + ", AstrixBeanId=" + id);
		}
		notifyBound();
		releaseInstanceIfBound(previousState);
	}

	private void releaseInstanceIfBound(InvocationHandler previousState) {
		if (previousState.getClass().equals(Bound.class)) {
			Bound<T> boundState = (Bound<T>) previousState;
			boundState.releaseInstance();
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
	
	private Bound<T> create(AstrixServiceProperties serviceProperties) {
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Misssing entry in service-registry beanKey=%s", beanKey));
		}
		AstrixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return new Bound<>(serviceComponent.bind(versioningContext, beanKey.getBeanType(), serviceProperties));
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
					throw new RuntimeException("Bean with key=" + beanKey + " was not bound before timeout");
				}
			}
		} finally {
			boundStateLock.unlock();
		}
	}
	
	public boolean isBound() {
		return !this.state.getClass().equals(Unbound.class);
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
		return this.state.invoke(proxy, method, args);
	}
	
	private class IllegalSubsystemState implements InvocationHandler {
		
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
	}
	
	private static class Bound<T> implements InvocationHandler {

		private final BoundServiceBeanInstance<T> serviceBeanInstance;
		
		public Bound(BoundServiceBeanInstance<T> bean) {
			this.serviceBeanInstance = bean;
		}

		public void releaseInstance() {
			serviceBeanInstance.release();
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
	}
	
	private class Unbound implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new ServiceUnavailableException("AstrixBeanId=" + id + " bean="+ beanKey);
		}
	}	
}
