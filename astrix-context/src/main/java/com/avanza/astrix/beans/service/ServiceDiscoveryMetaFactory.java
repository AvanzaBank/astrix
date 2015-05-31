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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.provider.core.AstrixServiceRegistryDiscovery;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceDiscoveryMetaFactory {
	
	private final ConcurrentMap<Class<?>, ServiceDiscoveryMetaFactoryPlugin<?>> discoveryStrategyByAnnotationType = new ConcurrentHashMap<>();
	
	public ServiceDiscoveryMetaFactory(List<ServiceDiscoveryMetaFactoryPlugin<?>> serviceDiscoveryPlugins) {
		for (ServiceDiscoveryMetaFactoryPlugin<?> lookupPlugin : serviceDiscoveryPlugins) {
			this.discoveryStrategyByAnnotationType.put(lookupPlugin.getDiscoveryAnnotationType(), lookupPlugin);
		}
	}
	
	public ServiceDiscoveryFactory<?> createServiceDiscoveryFactory(AstrixBeanKey<?> beanKey, AnnotatedElement annotatedElement) {
		ServiceDiscoveryMetaFactoryPlugin<?> servcieLookupPlugin = getServiceLookupPlugin(annotatedElement);
		if (servcieLookupPlugin != null) {
			return create(beanKey, annotatedElement, servcieLookupPlugin);
		}
		throw new IllegalArgumentException("Can't identify what lookup-strategy to use to locate services exported using annotated element: " + annotatedElement);
	}
	
	private <T extends Annotation> ServiceDiscoveryFactory<?> create(AstrixBeanKey<?> beanKey, AnnotatedElement annotatedElement, ServiceDiscoveryMetaFactoryPlugin<T> lookupPlugin) {
		T annotation = annotatedElement.getAnnotation(lookupPlugin.getDiscoveryAnnotationType());
		return new ServiceDiscoveryFactory<>(lookupPlugin, annotation);
	}
	
	private ServiceDiscoveryMetaFactoryPlugin<?> getServiceLookupPlugin(AnnotatedElement annotatedElement) {
		Class<?> discoveryStrategy = getLookupStrategy(annotatedElement);
		return discoveryStrategyByAnnotationType.get(discoveryStrategy);
	}
	
	public Class<?> getLookupStrategy(AnnotatedElement annotatedElement) {
		for (ServiceDiscoveryMetaFactoryPlugin<?> discoveryPlugin : discoveryStrategyByAnnotationType.values()) {
			if (annotatedElement.isAnnotationPresent(discoveryPlugin.getDiscoveryAnnotationType())) {
				return discoveryPlugin.getDiscoveryAnnotationType();
			}
		}
		return AstrixServiceRegistryDiscovery.class;
	}

}