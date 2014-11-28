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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Uses classpath scanning to locate api-descriptors. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApiDescriptorScanner implements AstrixApiDescriptors {

	private final Logger log = LoggerFactory.getLogger(AstrixApiDescriptorScanner.class);
	
	private static final Map<String, List<AstrixApiDescriptor>> apiDescriptorsByBasePackage = new HashMap<String, List<AstrixApiDescriptor>>();
	private List<String> basePackages = new ArrayList<>();
	
	public AstrixApiDescriptorScanner(String... basePackages) {
		this.basePackages.add("com.avanza.astrix"); // Always scan for com.avanza.astrix package
		this.basePackages.addAll(Arrays.asList(basePackages));
	}
	
	@Override
	public List<AstrixApiDescriptor> getAll() {
		List<AstrixApiDescriptor> result = new ArrayList<>();
		for (String basePackage : this.basePackages) {
			result.addAll(scanPackage(basePackage));
		}
		return result;
	}

	private List<AstrixApiDescriptor> scanPackage(String basePackage) {
		List<AstrixApiDescriptor> descriptors = apiDescriptorsByBasePackage.get(basePackage);
		if (descriptors != null) {
			log.debug("Returning cached api-descriptors found on earlier scan types={}", descriptors);
			return descriptors;
		}
		List<Class<? extends Annotation>> allProviderAnnotationTypes = getAllProviderAnnotationTypes();
		log.debug("Running scan for api-descriptors of types={}", allProviderAnnotationTypes);
		List<AstrixApiDescriptor> discoveredApiDescriptors = new ArrayList<>();
		Reflections reflections = new Reflections(basePackage);
		for (Class<? extends Annotation> apiAnnotation : allProviderAnnotationTypes) { 
			for (Class<?> providerClass : reflections.getTypesAnnotatedWith(apiAnnotation)) {
				AstrixApiDescriptor descriptor = AstrixApiDescriptor.create(providerClass);
				log.debug("Found api descriptor {}", descriptor);
				discoveredApiDescriptors.add(descriptor);
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
		for (AstrixApiProviderPlugin plugin : AstrixPluginDiscovery.discoverAllPlugins(AstrixApiProviderPlugin.class)) {
			result.add(plugin.getProviderAnnotationType());
		}
		return result;
	}

}
