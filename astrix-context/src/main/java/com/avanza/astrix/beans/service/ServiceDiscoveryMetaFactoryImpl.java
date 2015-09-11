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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class ServiceDiscoveryMetaFactoryImpl implements ServiceDiscoveryMetaFactory {
	
	private final ConcurrentMap<Class<?>, ServiceDiscoveryMetaFactoryPlugin<?>> discoveryStrategyByPropertiesType = new ConcurrentHashMap<>();
	
	public ServiceDiscoveryMetaFactoryImpl(List<ServiceDiscoveryMetaFactoryPlugin<?>> serviceDiscoveryPlugins) {
		for (ServiceDiscoveryMetaFactoryPlugin<?> discoveryPlugin : serviceDiscoveryPlugins) {
			this.discoveryStrategyByPropertiesType.put(discoveryPlugin.getDiscoveryPropertiesType(), discoveryPlugin);
		}
	}
	
	@Override
	public <T> ServiceDiscoveryFactory<T> createServiceDiscoveryFactory(Class<?> beanTypeToDiscovery, T discoveryProperties) {
		ServiceDiscoveryMetaFactoryPlugin<T> servcieLookupPlugin = getServiceDiscoveryPlugin(discoveryProperties);
		if (servcieLookupPlugin != null) {
			return create(beanTypeToDiscovery, discoveryProperties, servcieLookupPlugin);
		}
		throw new IllegalArgumentException("Can't identify what discovery-strategy to use for discovery properties type: " + discoveryProperties.getClass().getName());
	}
	
	private <T> ServiceDiscoveryFactory<T> create(Class<?> lookupBeanType, T discoveryProperties, ServiceDiscoveryMetaFactoryPlugin<T> discoveryPlugin) {
		return new ServiceDiscoveryFactory<>(discoveryPlugin, discoveryProperties, lookupBeanType);
	}
	
	@SuppressWarnings("unchecked")
	private <T> ServiceDiscoveryMetaFactoryPlugin<T> getServiceDiscoveryPlugin(T discoveryProperties) {
		return (ServiceDiscoveryMetaFactoryPlugin<T>) discoveryStrategyByPropertiesType.get(discoveryProperties.getClass());
	}

}