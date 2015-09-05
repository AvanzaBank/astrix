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
package com.avanza.astrix.serviceunit;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.publish.ApiProviderClass;

class ServiceProviderPlugins {
	
	private final ConcurrentMap<Class<? extends Annotation>, ServiceProviderPlugin> pluginByAnnotationType = new ConcurrentHashMap<>();
	
	public ServiceProviderPlugins(List<ServiceProviderPlugin> apiProviderPlugins) {
		for (ServiceProviderPlugin plugin : apiProviderPlugins) {
			ServiceProviderPlugin previous = this.pluginByAnnotationType.putIfAbsent(plugin.getProviderAnnotationType(), plugin);
			if (previous != null) {
				throw new IllegalArgumentException(String.format("Multiple ServiceProviderPlugin's found for providerAnnotationType=%s. p1=%s p2=%s", 
						plugin.getProviderAnnotationType().getName(), plugin.getClass().getName(), previous.getClass().getName()));
			}
		}
	}
	
	public List<ExportedServiceBeanDefinition<?>> getExportedServices(ApiProviderClass apiProvider) {
		ServiceProviderPlugin apiProviderPlugin = getProviderPlugin(apiProvider);
		List<ExportedServiceBeanDefinition<?>> result = new ArrayList<>();
		result.addAll(apiProviderPlugin.getExportedServices(apiProvider));
		return result;
	}
	
	private ServiceProviderPlugin getProviderPlugin(ApiProviderClass apiProvider) {
		for (ServiceProviderPlugin plugin : pluginByAnnotationType.values()) {
			if (apiProvider.isAnnotationPresent(plugin.getProviderAnnotationType())) {
				return plugin;
			}
		}
		throw new IllegalArgumentException("No plugin registered that can handle ApiProvider class: " + apiProvider + ".\n"
										 + "Some possible causes: \n"
										 + "1. You forgot annotate the ApiProvider class with an ApiProvider annotation, most commonly @AstrixApiProvider\n"
										 + "2. The given class is not an ApiProvider but rather a service interface. In that case you sould\n"
										 + "   update your applicationDescriptor's exportsRemoteServicesFor attribute (@AstrixApplication.exportsRemoteServicesFor) \n"
										 + "   to point to the ApiProvider rather than the service interface\n"
										 + "3. You don't have the Plugin that handles your ApiProvider on the classpath");
	}
}
