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
package com.avanza.astrix.context;

import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviders;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

/**
 * Uses classpath scanning to find api-providers. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApiProviderClassScanner implements ApiProviders {

	private final Logger log = LoggerFactory.getLogger(AstrixApiProviderClassScanner.class);
	
	private static final Map<String, List<ApiProviderClass>> apiProvidersByBasePackage = new ConcurrentHashMap<>();
	private final List<String> basePackages = new ArrayList<>();
	private final List<Class<? extends Annotation>> providerAnnotationsToScanFor;
	
	public AstrixApiProviderClassScanner(List<Class<? extends Annotation>> providerAnnotationsToScanFor, String basePackage, String... otherBasePackages) {
		this.providerAnnotationsToScanFor = providerAnnotationsToScanFor;
		this.basePackages.add(basePackage);
		this.basePackages.addAll(Arrays.asList(otherBasePackages));
	}
	
	@Override
	public Stream<ApiProviderClass> getAll() {
		return basePackages.stream()
						   .flatMap(this::scanPackage);
	}

	private Stream<ApiProviderClass> scanPackage(String basePackage) {
		log.debug("Scanning package for api-providers: package={}", basePackage);
		List<ApiProviderClass> providerClasses = apiProvidersByBasePackage.get(basePackage);
		if (providerClasses != null) {
			log.debug("Returning cached api-providers found on earlier scan types={}", providerClasses);
			return providerClasses.stream();
		}
		List<Class<? extends Annotation>> allProviderAnnotationTypes = getAllProviderAnnotationTypes();
		log.debug("Running scan for api-providers of types={}", allProviderAnnotationTypes);

		Reflections reflections = new Reflections(basePackage);
		List<ApiProviderClass> discoveredApiProviders = allProviderAnnotationTypes.stream()
				.flatMap(apiAnnotation -> getTypesAnnotatedWith(reflections, apiAnnotation).stream())
				.map(ApiProviderClass::create)
				.peek(provider -> log.debug("Found api provider {}", provider))
				.collect(toList());
		apiProvidersByBasePackage.put(basePackage, discoveredApiProviders);
		return discoveredApiProviders.stream();
	}
	
	void addBasePackage(String basePackage) {
		this.basePackages.add(basePackage);
	}

	private List<Class<? extends Annotation>> getAllProviderAnnotationTypes() {
		return providerAnnotationsToScanFor;
	}

	private Set<Class<?>> getTypesAnnotatedWith(Reflections reflections, Class<? extends Annotation> annotation) {
		try {
			return reflections.getTypesAnnotatedWith(annotation);
		} catch (ReflectionsException exception) {
			// Reflections (0.9.12) throws ReflectionsException if nothing found in package
			log.trace("Could not retrieve types annotated with {}", annotation, exception);
			return emptySet();
		}
	}
}
