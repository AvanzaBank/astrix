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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AstrixServiceProviderFactory {
	
	private final Logger log = LoggerFactory.getLogger(AstrixServiceProviderFactory.class);
	private ConcurrentMap<Class<? extends Annotation>, AstrixServiceProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public AstrixServiceProviderFactory(AstrixContext context) {
		for (AstrixServiceProviderPlugin plugin : context.getPlugins(AstrixServiceProviderPlugin.class)) {
			context.injectDependencies(plugin);
			AstrixServiceProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				// TODO: how to handle multiple providers for same annotation type? Is it allowed
				log.warn("Multiple AstrixServiceProviderPlugin's found for annotation={}. p1={} p2={}", new Object[]{plugin, previous});
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
		for (AstrixServiceProviderPlugin plugin : pluginByAnnotationType.values()) {
			if (plugin.consumes(descriptorHolder)) {
				// TODO: what if multiple plugins consumes same annotation?
				return plugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle descriptor: " + descriptorHolder);
	}
	
}
