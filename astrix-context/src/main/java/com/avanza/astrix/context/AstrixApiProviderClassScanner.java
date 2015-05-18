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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviders;
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
	public List<ApiProviderClass> getAll() {
		List<ApiProviderClass> result = new ArrayList<>();
		for (String basePackage : this.basePackages) {
			result.addAll(scanPackage(basePackage));
		}
		return result;
	}

	private List<ApiProviderClass> scanPackage(String basePackage) {
		log.debug("Scanning package for api-providers: package={}", basePackage);
		List<ApiProviderClass> providerClasses = apiProvidersByBasePackage.get(basePackage);
		if (providerClasses != null) {
			log.debug("Returning cached api-providers found on earlier scan types={}", providerClasses);
			return providerClasses;
		}
		List<Class<? extends Annotation>> allProviderAnnotationTypes = getAllProviderAnnotationTypes();
		log.debug("Running scan for api-providers of types={}", allProviderAnnotationTypes);
		List<ApiProviderClass> discoveredApiPRoviders = new ArrayList<>();
		Reflections reflections = new Reflections(basePackage);
		for (Class<? extends Annotation> apiAnnotation : allProviderAnnotationTypes) { 
			for (Class<?> providerClass : reflections.getTypesAnnotatedWith(apiAnnotation)) {
				ApiProviderClass provider = ApiProviderClass.create(providerClass);
				log.debug("Found api provider {}", provider);
				discoveredApiPRoviders.add(provider);
			}
		}
		apiProvidersByBasePackage.put(basePackage, discoveredApiPRoviders);
		return discoveredApiPRoviders;
	}
	
	void addBasePackage(String basePackage) {
		this.basePackages.add(basePackage);
	}

	private List<Class<? extends Annotation>> getAllProviderAnnotationTypes() {
		return providerAnnotationsToScanFor;
	}

}
