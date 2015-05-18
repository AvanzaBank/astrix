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
package com.avanza.astrix.serviceunit;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.publish.ApiProviderClass;

public class ServiceProviderPlugins {
	
	private final ConcurrentMap<Class<? extends Annotation>, ServiceProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public ServiceProviderPlugins(Collection<ServiceProviderPlugin> apiProviderPlugins) {
		for (ServiceProviderPlugin plugin : apiProviderPlugins) {
			ServiceProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				throw new IllegalArgumentException(String.format("Multiple ServiceProviderPlugin's found for providerAnnotationType=%s. p1=%s p2=%s", 
						plugin.getProviderAnnotationType().getName(), plugin.getClass().getName(), previous.getClass().getName()));
			}
		}
	}
	
	public List<ServiceBeanDefinition> getExportedServices(ApiProviderClass apiProvider) {
		ServiceProviderPlugin apiProviderPlugin = getProviderPlugin(apiProvider);
		List<ServiceBeanDefinition> result = new ArrayList<>();
		result.addAll(apiProviderPlugin.getProvidedServices(apiProvider));
		return result;
	}
	
	private ServiceProviderPlugin getProviderPlugin(ApiProviderClass apiProvider) {
		for (ServiceProviderPlugin plugin : pluginByAnnotationType.values()) {
			if (apiProvider.isAnnotationPresent(plugin.getProviderAnnotationType())) {
				return plugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle apiProvider: " + apiProvider);
	}
}
