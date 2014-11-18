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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import com.avanza.astrix.context.AsterixServiceProperties;
import com.avanza.astrix.core.ServiceUnavailableException;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public final class LeasedService<T> implements InvocationHandler {
	
	private volatile T currentInstance;
	private volatile AsterixServiceProperties currentProperties;
	private final ServiceRegistryLookupFactory<T> factoryBean;
	private final String qualifier;
	
	public LeasedService(T currentInstance,
			AsterixServiceProperties currentProperties,
			String qualifier,
			ServiceRegistryLookupFactory<T> factoryBean) {
		this.currentInstance = currentInstance;
		this.currentProperties = currentProperties;
		this.qualifier = qualifier;
		this.factoryBean = factoryBean;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(currentInstance, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	public void refreshServiceProperties(AsterixServiceProperties serviceProperties) {
		// TODO: if this fails, make sure new attempts are performed later
		if (serviceHasChanged(serviceProperties)) {
			if (serviceProperties != null) {
				currentInstance = factoryBean.create(qualifier, serviceProperties);
			} else {
				currentInstance = (T) Proxy.newProxyInstance(factoryBean.getBeanType().getClassLoader(), new Class[]{getBeanType()}, new NotRegistered());
			}
			currentProperties = serviceProperties;
		}
	}

	private boolean serviceHasChanged(AsterixServiceProperties serviceProperties) {
		return !Objects.equals(currentProperties, serviceProperties);
	}


	public Class<T> getBeanType() {
		return factoryBean.getBeanType();
	}

	public String getQualifier() {
		return this.qualifier;
	}
	
	private class NotRegistered implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new ServiceUnavailableException("Service not available in registry. type=" + getBeanType().getName() + ", qualifier=" + getQualifier());
		}
	}
	
}
