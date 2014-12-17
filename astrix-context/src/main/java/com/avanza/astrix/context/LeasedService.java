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
package com.avanza.astrix.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import com.avanza.astrix.core.ServiceUnavailableException;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
class LeasedService<T> implements InvocationHandler {

	private AstrixServiceLookup serviceLookup;
	private volatile T currentInstance;
	private volatile AstrixServiceProperties currentProperties;
	private AstrixServiceFactory<T> serviceFactory;
	private String qualifier;

	public LeasedService(T currentInstance, 
			String qualifier,
			AstrixServiceProperties currentProperties,
			AstrixServiceFactory<T> serviceFactory,
			AstrixServiceLookup serviceLookup) {
		this.currentInstance = currentInstance;
		this.qualifier = qualifier;
		this.currentProperties = currentProperties;
		this.serviceFactory = serviceFactory;
		this.serviceLookup = serviceLookup;
	}

	// TODO: rename to getBeanKey
	public AstrixBeanKey<T> getBeanType() {
		return serviceFactory.getBeanKey();
	}
	
	// TODO: rename to getBeanType
	public Class<T> getType() {
		return serviceFactory.getBeanKey().getBeanType();
	}
	
	public String getQualifier() {
		return qualifier;
	}

	public void renew() {
		AstrixServiceProperties serviceProperties = serviceLookup.lookup(getType(), getQualifier());
		refreshServiceProperties(serviceProperties);
	}

	private void refreshServiceProperties(AstrixServiceProperties serviceProperties) {
		if (serviceHasChanged(serviceProperties)) {
			if (serviceProperties != null) {
				currentInstance = serviceFactory.create(qualifier, serviceProperties);
			} else {
				currentInstance = (T) Proxy.newProxyInstance(getType().getClassLoader(), new Class[]{getType()}, new NotRegistered());
			}
			currentProperties = serviceProperties;
		}
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(currentInstance, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	

	private boolean serviceHasChanged(AstrixServiceProperties serviceProperties) {
		return !Objects.equals(currentProperties, serviceProperties);
	}

	private class NotRegistered implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new ServiceUnavailableException("Service not available in registry. type=" + getType().getName() + ", qualifier=" + getQualifier());
		}
	}

}
