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
package com.avanza.asterix.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
/**
 * Uses classpath scanning to locate api-descriptors. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixApiDescriptorScanner implements AsterixApiDescriptors {

	private final Logger log = LoggerFactory.getLogger(AsterixApiDescriptorScanner.class);
	
	private static final Map<String, List<AsterixApiDescriptor>> apiDescriptorsByBasePackage = new HashMap<String, List<AsterixApiDescriptor>>();
	private List<String> basePackages = new ArrayList<>();
	
	public AsterixApiDescriptorScanner(String... basePackages) {
		this.basePackages.addAll(Arrays.asList(basePackages));
	}
	
	@Override
	public List<AsterixApiDescriptor> getAll() {
		List<AsterixApiDescriptor> result = new ArrayList<>();
		for (String basePackage : this.basePackages) {
			result.addAll(scanPackage(basePackage));
		}
		return result;
	}

	private List<AsterixApiDescriptor> scanPackage(String basePackage) {
		List<AsterixApiDescriptor> descriptors = apiDescriptorsByBasePackage.get(basePackage);
		if (descriptors != null) {
			log.debug("Returning cached api-descriptors found on earlier scan types={}", descriptors);
			return descriptors;
		}
		List<Class<? extends Annotation>> allProviderAnnotationTypes = getAllProviderAnnotationTypes();
		log.debug("Running scan for api-descriptors of types={}", allProviderAnnotationTypes);
		List<AsterixApiDescriptor> discoveredApiDescriptors = new ArrayList<>();
		ClassPathScanningCandidateComponentProvider providerScanner = new ClassPathScanningCandidateComponentProvider(false);
		for (Class<? extends Annotation> providerFactory : allProviderAnnotationTypes) {
			providerScanner.addIncludeFilter(new AnnotationTypeFilter(providerFactory));
		}
		Set<BeanDefinition> foundCandidateComponents = providerScanner.findCandidateComponents(basePackage);
		for (BeanDefinition beanDefinition : foundCandidateComponents) {
			try {
				Class<?> providerClass = Class.forName(beanDefinition.getBeanClassName());
				AsterixApiDescriptor descriptor = AsterixApiDescriptor.create(providerClass);
				log.debug("Found api descriptor {}", descriptor);
				discoveredApiDescriptors.add(descriptor);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Unable to load api descriptor class " + beanDefinition.getBeanClassName(), e);
			}
		}
		apiDescriptorsByBasePackage.put(basePackage, discoveredApiDescriptors);
		return discoveredApiDescriptors;
	}
	
	void addBasePackage(String basePackage) {
		this.basePackages.add(basePackage);
	}

	private List<Class<? extends Annotation>> getAllProviderAnnotationTypes() {
		List<Class<? extends Annotation>> result = new ArrayList<>();
		for (AsterixApiProviderPlugin plugin : AsterixPluginDiscovery.discoverAllPlugins(AsterixApiProviderPlugin.class)) {
			result.add(plugin.getProviderAnnotationType());
		}
		return result;
	}
}
