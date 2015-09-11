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

import java.lang.reflect.Proxy;
import java.util.Objects;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.DynamicFactoryBean;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class ServiceFactory<T> implements DynamicFactoryBean<T> {

	private final ServiceDefinition<T> serviceDefinition;
	private final ServiceBeanContext serviceBeanContext;
	private final ServiceDiscoveryFactory<?> serviceDiscoveryFactory;

	ServiceFactory(ServiceDefinition<T> serviceDefinition, 
						  ServiceBeanContext serviceBeanContext, ServiceDiscoveryFactory<?> serviceDiscoveryFactory) {
		this.serviceDiscoveryFactory = Objects.requireNonNull(serviceDiscoveryFactory);
		this.serviceDefinition = Objects.requireNonNull(serviceDefinition);
		this.serviceBeanContext = Objects.requireNonNull(serviceBeanContext);
	}

	public T create(AstrixBeanKey<T> beanKey) {
		ServiceDiscovery serviceDiscovery = serviceDiscoveryFactory.create(beanKey.getQualifier());
		ServiceBeanInstance<T> serviceBeanInstance = ServiceBeanInstance.create(serviceDefinition, beanKey, serviceDiscovery, serviceBeanContext);
		serviceBeanInstance.bind();
		serviceBeanContext.getLeaseManager().startManageLease(serviceBeanInstance);
		serviceBeanContext.getServiceMbeanExporter().register(serviceBeanInstance);
		return beanKey.getBeanType().cast(
				Proxy.newProxyInstance(beanKey.getBeanType().getClassLoader(), 
									   new Class[]{beanKey.getBeanType(), StatefulAstrixBean.class}, 
									   serviceBeanInstance));
	}
	
	@Override
	public Class<T> getType() {
		return serviceDefinition.getServiceType();
	}
	
}
