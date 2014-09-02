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
 * This is a component used to create runtime factory representations (AstrixApiProvider) for api's hooked
 * into astrix. Given an descriptorHolder this factory creates an AstrixApiProvider for a given api. <p>
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApiProviderFactory {
	
	private final Logger log = LoggerFactory.getLogger(AstrixApiProviderFactory.class);
	private final ConcurrentMap<Class<? extends Annotation>, AstrixApiProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public AstrixApiProviderFactory(Collection<AstrixApiProviderPlugin> apiProviderPlugins) {
		for (AstrixApiProviderPlugin plugin : apiProviderPlugins) {
			AstrixApiProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				// TODO: how to handle multiple providers for same annotation type? Is it allowed
				log.warn("Multiple AstrixApiProviderPlugin's found for annotation={}. p1={} p2={}", new Object[]{plugin, previous});
			}
		}
	}
	
	public Set<Class<? extends Annotation>> getProvidedAnnotationTypes() {
		return pluginByAnnotationType.keySet();
	}
	
	public AstrixApiProvider create(Class<?> descriptorHolder) {
		// TODO: descriptor is not the actual annotation type, but rather the class holding the given annotation
		AstrixApiProviderPlugin providerFactoryPlugin = getProviderFactoryPlugin(descriptorHolder);
		List<AstrixFactoryBean<?>> factoryBeans = providerFactoryPlugin.createFactoryBeans(descriptorHolder);
		return new AstrixApiProvider(factoryBeans, descriptorHolder); 
	}

	private AstrixApiProviderPlugin getProviderFactoryPlugin(Class<?> descriptorHolder) {
		for (AstrixApiProviderPlugin plugin : pluginByAnnotationType.values()) {
			if (plugin.consumes(descriptorHolder)) {
				// TODO: what if multiple plugins consumes same annotation?
				return plugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle descriptor: " + descriptorHolder);
	}
	
}
