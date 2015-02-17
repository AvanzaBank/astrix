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

import com.avanza.astrix.beans.core.AstrixApiDescriptor;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceLookupFactory {
	
	private final ConcurrentMap<Class<?>, AstrixServiceLookupPlugin<?>> lookupStrategyByAnnotationType = new ConcurrentHashMap<>();
	
	public AstrixServiceLookupFactory(List<AstrixServiceLookupPlugin<?>> serviceLookupPlugins) {
		for (AstrixServiceLookupPlugin<?> lookupPlugin : serviceLookupPlugins) {
			this.lookupStrategyByAnnotationType.put(lookupPlugin.getLookupAnnotationType(), lookupPlugin);
		}
	}
	

	public AstrixServiceLookup createServiceLookup(AnnotatedElement annotatedElement) {
		AstrixServiceLookupPlugin<?> servcieLookupPlugin = getServiceLookupPlugin(annotatedElement);
		if (servcieLookupPlugin != null) {
			return create(annotatedElement, servcieLookupPlugin);
		}
		throw new IllegalArgumentException("Can't identify what lookup-strategy to use to locate services exported using annotated element: " + annotatedElement);
	}
	
	private <T extends Annotation> AstrixServiceLookup create(AnnotatedElement annotatedElement, AstrixServiceLookupPlugin<T> lookupPlugin) {
		return AstrixServiceLookup.create(lookupPlugin, annotatedElement.getAnnotation(lookupPlugin.getLookupAnnotationType()));
	}
	
	private AstrixServiceLookupPlugin<?> getServiceLookupPlugin(AnnotatedElement annotatedElement) {
		Class<?> lookupStrategy = getLookupStrategy(annotatedElement);
		return lookupStrategyByAnnotationType.get(lookupStrategy);
	}
	

	public boolean usesServiceRegistry(AstrixApiDescriptor apiDescriptor) {
		return getLookupStrategy(apiDescriptor.getDescriptorClass()).equals(AstrixServiceRegistryLookup.class);
	}
	
	public Class<?> getLookupStrategy(AnnotatedElement annotatedElement) {
		for (AstrixServiceLookupPlugin<?> lookupPlugin : lookupStrategyByAnnotationType.values()) {
			if (annotatedElement.isAnnotationPresent(lookupPlugin.getLookupAnnotationType())) {
				return lookupPlugin.getLookupAnnotationType();
			}
		}
		return AstrixServiceRegistryLookup.class;
	}

}