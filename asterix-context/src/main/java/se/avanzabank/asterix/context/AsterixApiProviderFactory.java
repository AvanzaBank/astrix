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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a component used to create runtime factory representations (AsterixApiProvider) for api's hooked
 * into asterix. Given an descriptorHolder this factory creates an AsterixApiProvider for a given api. <p>
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixApiProviderFactory {
	
	private final Logger log = LoggerFactory.getLogger(AsterixApiProviderFactory.class);
	private final ConcurrentMap<Class<? extends Annotation>, AsterixApiProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public AsterixApiProviderFactory(Collection<AsterixApiProviderPlugin> apiProviderPlugins) {
		for (AsterixApiProviderPlugin plugin : apiProviderPlugins) {
			AsterixApiProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				// TODO: how to handle multiple providers for same annotation type? Is it allowed
				log.warn("Multiple AsterixApiProviderPlugin's found for annotation={}. p1={} p2={}", new Object[]{plugin, previous});
			}
		}
	}
	
	public Set<Class<? extends Annotation>> getProvidedAnnotationTypes() {
		return pluginByAnnotationType.keySet();
	}
	
	public AsterixApiProvider create(AsterixApiDescriptor descriptor) {
		AsterixApiProviderPlugin providerFactoryPlugin = getProviderPlugin(descriptor);
		List<AsterixFactoryBean<?>> factoryBeans = providerFactoryPlugin.createFactoryBeans(descriptor);
		return new AsterixApiProvider(factoryBeans, descriptor); 
	}

	private AsterixApiProviderPlugin getProviderPlugin(AsterixApiDescriptor descriptor) {
		for (AsterixApiProviderPlugin plugin : pluginByAnnotationType.values()) {
			if (plugin.consumes(descriptor)) {
				// TODO: what if multiple plugins consumes same annotation?
				return plugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle descriptor: " + descriptor);
	}
	
}
