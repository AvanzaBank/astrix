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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceLookupMetaFactory {
	
	private final ConcurrentMap<Class<?>, ServiceLookupMetaFactoryPlugin<?>> lookupStrategyByAnnotationType = new ConcurrentHashMap<>();
	
	public ServiceLookupMetaFactory(List<ServiceLookupMetaFactoryPlugin<?>> serviceLookupPlugins) {
		for (ServiceLookupMetaFactoryPlugin<?> lookupPlugin : serviceLookupPlugins) {
			this.lookupStrategyByAnnotationType.put(lookupPlugin.getLookupAnnotationType(), lookupPlugin);
		}
	}
	
	public ServiceLookupFactory<?> createServiceLookup(AstrixBeanKey<?> beanKey, AnnotatedElement annotatedElement) {
		ServiceLookupMetaFactoryPlugin<?> servcieLookupPlugin = getServiceLookupPlugin(annotatedElement);
		if (servcieLookupPlugin != null) {
			return create(beanKey, annotatedElement, servcieLookupPlugin);
		}
		throw new IllegalArgumentException("Can't identify what lookup-strategy to use to locate services exported using annotated element: " + annotatedElement);
	}
	
	private <T extends Annotation> ServiceLookupFactory<?> create(AstrixBeanKey<?> beanKey, AnnotatedElement annotatedElement, ServiceLookupMetaFactoryPlugin<T> lookupPlugin) {
		T annotation = annotatedElement.getAnnotation(lookupPlugin.getLookupAnnotationType());
		return new ServiceLookupFactory<>(lookupPlugin, annotation);
	}
	
	private ServiceLookupMetaFactoryPlugin<?> getServiceLookupPlugin(AnnotatedElement annotatedElement) {
		Class<?> lookupStrategy = getLookupStrategy(annotatedElement);
		return lookupStrategyByAnnotationType.get(lookupStrategy);
	}
	
	public Class<?> getLookupStrategy(AnnotatedElement annotatedElement) {
		for (ServiceLookupMetaFactoryPlugin<?> lookupPlugin : lookupStrategyByAnnotationType.values()) {
			if (annotatedElement.isAnnotationPresent(lookupPlugin.getLookupAnnotationType())) {
				return lookupPlugin.getLookupAnnotationType();
			}
		}
		return AstrixServiceRegistryLookup.class;
	}

}