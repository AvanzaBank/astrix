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

import java.lang.reflect.Proxy;
import java.util.Objects;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.DynamicFactoryBean;
import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.config.DynamicConfig;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class ServiceFactory<T> implements DynamicFactoryBean<T> {

	private final ServiceComponents serviceComponents;
	private final ServiceDiscoveryFactory<?> serviceDiscoveryFactory;
	private final ServiceLeaseManager leaseManager;
	private final ServiceContext versioningContext;
	private final DynamicConfig config;
	private final Class<T> type;

	public ServiceFactory(ServiceContext versioningContext, 
								ServiceDiscoveryFactory<?> serviceDiscoveryFactory, 
								ServiceComponents serviceComponents, 
								ServiceLeaseManager leaseManager,
								DynamicConfig config,
								Class<T> type) {
		this.config = config;
		this.versioningContext = Objects.requireNonNull(versioningContext);
		this.serviceDiscoveryFactory = Objects.requireNonNull(serviceDiscoveryFactory);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.leaseManager = Objects.requireNonNull(leaseManager);
		this.type = Objects.requireNonNull(type);
	}

	public T create(AstrixBeanKey<T> beanKey) {
		ServiceDiscovery serviceDiscovery = serviceDiscoveryFactory.create(beanKey);
		ServiceBeanInstance<T> serviceBeanInstance = ServiceBeanInstance.create(versioningContext, beanKey, serviceDiscovery, serviceComponents, config);
		serviceBeanInstance.bind();
		leaseManager.startManageLease(serviceBeanInstance);
		return beanKey.getBeanType().cast(
				Proxy.newProxyInstance(beanKey.getBeanType().getClassLoader(), 
									   new Class[]{beanKey.getBeanType(), StatefulAstrixBean.class}, 
									   serviceBeanInstance));
	}
	
	@Override
	public Class<T> getType() {
		return type;
	}

	public static <T> FactoryBean<T> dynamic(ServiceContext serviceContext, 
													Class<T> beanType, 
													ServiceDiscoveryFactory<?> serviceDiscovery, 
													ServiceComponents serviceComponents, 
													ServiceLeaseManager leaseManager,
													DynamicConfig config) {
		return new ServiceFactory<T>(serviceContext, serviceDiscovery, serviceComponents, leaseManager, config, beanType);
	}
	
	public static <T> FactoryBean<T> standard(ServiceContext serviceContext, 
													AstrixBeanKey<T> beanType, 
													ServiceDiscoveryFactory<?> serviceDiscoveryFactory, 
													ServiceComponents serviceComponents, 
													ServiceLeaseManager leaseManager,
													DynamicConfig config) {
		ServiceFactory<T> serviceFactory = new ServiceFactory<T>(serviceContext, serviceDiscoveryFactory, serviceComponents, leaseManager, config, beanType.getBeanType());
		return new FactoryBeanAdapter<T>(serviceFactory, beanType);
	}
	
	private static class FactoryBeanAdapter<T> implements StandardFactoryBean<T> {

		private ServiceFactory<T> serviceFactory;
		private AstrixBeanKey<T> beanKey;
		
		public FactoryBeanAdapter(ServiceFactory<T> serviceFactory,
				AstrixBeanKey<T> beanKey) {
			this.serviceFactory = serviceFactory;
			this.beanKey = beanKey;
		}

		@Override
		public T create(AstrixBeans beans) {
			return serviceFactory.create(beanKey);
		}
		
		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
	}

}
