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
package se.avanzabank.service.suite.context;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



public class AstrixServiceProviderFactory {
	
	private ConcurrentMap<Class<? extends Annotation>, AstrixServiceProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public AstrixServiceProviderFactory(AstrixContext context, Astrix astrix) {
		for (AstrixServiceProviderPlugin plugin : context.getProviders(AstrixServiceProviderPlugin.class)) {
			plugin.setPlugins(context);
			plugin.setAstrix(astrix);
			AstrixServiceProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				// TODO: warning, multiple providers for same annotation type..
			}
		}
	}
	
	public Set<Class<? extends Annotation>> getProvidedAnnotaionTypes() {
		return pluginByAnnotationType.keySet();
	}
	
	
	public AstrixServiceProvider create(Class<?> descriptorHolder) {
		// TODO: descriptor is not the actual annotation type, but rather the class holding the given annotation
		AstrixServiceProviderPlugin providerFactoryPlugin = getProviderFactoryPlugin(descriptorHolder);
		return providerFactoryPlugin.create(descriptorHolder);
	}

	private AstrixServiceProviderPlugin getProviderFactoryPlugin(Class<?> descriptorHolder) {
		for (AstrixServiceProviderPlugin factoryPlugin : pluginByAnnotationType.values()) {
			if (descriptorHolder.isAnnotationPresent(factoryPlugin.getProviderAnnotationType())) {
				return factoryPlugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle descriptor: " + descriptorHolder);
	}
	
}
