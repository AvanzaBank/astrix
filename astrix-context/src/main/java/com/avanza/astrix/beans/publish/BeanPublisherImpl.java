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
package com.avanza.astrix.beans.publish;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.BeanConfigurations;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.BeanFactory;
import com.avanza.astrix.beans.factory.DynamicFactoryBean;
import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.service.ServiceDiscoveryDefinition;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactory;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactory;
import com.avanza.astrix.beans.service.ServiceFactory;
import com.avanza.astrix.beans.service.ServiceMetaFactory;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanPublisherImpl implements BeanPublisher {

	private static final Logger log = LoggerFactory.getLogger(BeanPublisherImpl.class);

	private final ApiProviderPluginsImpl apiProviderPluginsImpl;
	private final BeanFactory beanFactory;
	private final ServiceMetaFactory serviceMetaFactory;
	private final ServiceDiscoveryMetaFactory serviceDiscoveryMetaFactory;
	private final BeanConfigurations beanConfigurations;
	
	public BeanPublisherImpl(ApiProviderPluginsImpl apiProviderPluginsImpl, BeanFactory beanFactory,
			ServiceMetaFactory serviceMetaFactory, ServiceDiscoveryMetaFactory serviceDiscoveryMetaFactory,
			BeanConfigurations beanConfigurations) {
		this.apiProviderPluginsImpl = apiProviderPluginsImpl;
		this.beanFactory = beanFactory;
		this.serviceMetaFactory = serviceMetaFactory;
		this.serviceDiscoveryMetaFactory = serviceDiscoveryMetaFactory;
		this.beanConfigurations = beanConfigurations;
	}

	@Override
	public void publish(ApiProviderClass apiProvider) {
		log.debug("Registering apiProvider={}", apiProvider.getName());
		apiProviderPluginsImpl.getProviderPlugin(apiProvider).publishBeans(new BeanPublisherPlugin.BeanPublisher() {
			@Override
			public <T> void publishService(ServiceBeanDefinition<T> serviceBeanDefinition) {
				ServiceDiscoveryFactory<?> serviceDiscoveryFactory = createServiceDiscoveryFactory(serviceBeanDefinition.getServiceDiscoveryDefinition());
				ServiceFactory<T> serviceFactory = serviceMetaFactory.createServiceFactory(serviceBeanDefinition.getServiceDefinition(), serviceDiscoveryFactory);
				if (serviceBeanDefinition.getServiceDefinition().isDynamicQualified()) {
					log.debug("Registering dynamic service factory. beanType={} apiProvider={}", serviceBeanDefinition.getBeanKey().getBeanType(), apiProvider.getName());
					registerBean(serviceFactory, serviceBeanDefinition.getDefaultBeanSettingsOverride());  
				} else {
					log.debug("Registering factory for service-bean. beanKey={} apiProvider={}", serviceBeanDefinition.getBeanKey(), apiProvider.getName());
					registerBean(new FactoryBeanAdapter<>(serviceFactory, serviceBeanDefinition.getBeanKey()), serviceBeanDefinition.getDefaultBeanSettingsOverride());
				}
			}
			@Override
			public <T> void publishLibrary(LibraryBeanDefinition<T> libraryBeanDefinition) {
				log.debug("Registering factory for library-bean. beanKey={} apiProvider={}",  
						libraryBeanDefinition.getBeanKey(), 
						apiProvider.getName());
				registerBean(libraryBeanDefinition.getFactory(), libraryBeanDefinition.getDefaultBeanSettingsOverride());
			}
		}, apiProvider);
	}
	
	private void registerBean(FactoryBean<?> factory, Map<BeanSetting<?>, Object> defaultBeanSettingsOverride) {
		this.beanFactory.registerFactory(factory);
		this.beanConfigurations.setDefaultBeanConfig(getBeanKey(factory), defaultBeanSettingsOverride);
	}
	
	@SuppressWarnings("unchecked")
	private AstrixBeanKey<?> getBeanKey(FactoryBean<?> factory) {
		if (factory instanceof StandardFactoryBean) {
			return StandardFactoryBean.class.cast(factory).getBeanKey();
		}
		if (factory instanceof DynamicFactoryBean) {
			return AstrixBeanKey.create(DynamicFactoryBean.class.cast(factory).getType(), "*");
		}
		throw new RuntimeException("Unknown factory type: " + factory.getClass().getName());
	}
	
	
	private <T> ServiceDiscoveryFactory<?> createServiceDiscoveryFactory(ServiceDiscoveryDefinition serviceDiscoveryDefinition) {
		return serviceDiscoveryMetaFactory.createServiceDiscoveryFactory(serviceDiscoveryDefinition.getDiscoveryBeanKey().getBeanType(),
																		 serviceDiscoveryDefinition.getServiceDiscoveryProperties());
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
