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
package se.avanzabank.asterix.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * This component is used to create runtime factory representations (AsterixApiProvider) for api's hooked
 * into asterix. An API is defined by an AsterixApiDescriptor, which in turn uses different annotations for
 * different types of apis. This class is responsible for interpreting such annotations and create an
 * AsterixApiProvider for the given api. <p>
 * 
 * The factory is extendable by adding more {@link AsterixApiProviderPlugin}'s. <p>
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixApiProviderFactory {
	
	private final ConcurrentMap<Class<? extends Annotation>, AsterixApiProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	private final AsterixApiProviderPlugins apiProviderPlugins;
	
	public AsterixApiProviderFactory(AsterixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}
	
	public Set<Class<? extends Annotation>> getProvidedAnnotationTypes() {
		return pluginByAnnotationType.keySet();
	}
	
	public AsterixApiProvider create(AsterixApiDescriptor descriptor) {
		AsterixApiProviderPlugin providerFactoryPlugin = getProviderPlugin(descriptor);
		List<AsterixFactoryBean<?>> factoryBeans = new ArrayList<>();
		for (AsterixFactoryBeanPlugin<?> factoryBean : providerFactoryPlugin.createFactoryBeans(descriptor)) {
			if (providerFactoryPlugin.useStatefulBeanFactory()) {
				factoryBeans.add(new AsterixFactoryBean<>(new CachingAsterixFactoryBean<>(new StatefulAsterixFactoryBean<>(factoryBean))));
			} else {
				factoryBeans.add(new AsterixFactoryBean<>(new CachingAsterixFactoryBean<>(factoryBean)));
			}
		}
		return new AsterixApiProvider(factoryBeans, descriptor, providerFactoryPlugin.useStatefulBeanFactory()); 
	}
	
	private AsterixApiProviderPlugin getProviderPlugin(AsterixApiDescriptor descriptor) {
		return this.apiProviderPlugins.getProviderPlugin(descriptor);
	}
	
}
