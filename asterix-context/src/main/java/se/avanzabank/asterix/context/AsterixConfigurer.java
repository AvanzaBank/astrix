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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AsterixConfigurer {


	private static final Logger log = LoggerFactory.getLogger(AsterixConfigurer.class);
	
	private boolean useFaultTolerance = false;
	private boolean enableVersioning = true;
	private List<ExternalDependencyBean> externalDependencyBeans = new ArrayList<>();
	private List<Object> externalDependencies = new ArrayList<>();
	
	public AsterixContext configure() {
		AsterixContext context = new AsterixContext();
		context.setExternalDependencyBeans(externalDependencyBeans);
		context.setExternalDependencies(externalDependencies);
		configureFaultTolerance(context);
		configureVersioning(context);
		discoverApiProviderPlugins(context);
		List<AsterixApiProviderPlugin> apiProviderPlugins = context.getPlugins(AsterixApiProviderPlugin.class);
		AsterixApiProviderFactory apiProviderFactory = new AsterixApiProviderFactory(apiProviderPlugins);
		List<AsterixApiProvider> apiProviders = findAsterixApiProviders(apiProviderFactory, "se.avanzabank");
		for (AsterixApiProvider apiProvider : apiProviders) {
			context.registerApiProvider(apiProvider);
		}
		return context;
	}
	
	private List<AsterixApiProvider> findAsterixApiProviders(AsterixApiProviderFactory apiProviderFactory, String basePackage) {
		List<AsterixApiDescriptor> apiDescriptors = new AsterixApiAsterixApiDescriptorScanner(basePackage).scan();
		List<AsterixApiProvider> result = new ArrayList<>();
		for (AsterixApiDescriptor descriptor : apiDescriptors) {
			result.add(apiProviderFactory.create(descriptor));
		}
		return result;
	}

	@Autowired(required = false)
	public void setExternalDependencies(List<ExternalDependencyBean> externalDependencies) {
		this.externalDependencyBeans = externalDependencies;
	}
	
	public void useFaultTolerance(boolean useFaultTolerance) {
		this.useFaultTolerance  = useFaultTolerance;
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.enableVersioning = enableVersioning;
	}

	private void discoverApiProviderPlugins(AsterixContext context) {
		discoverAllPlugins(context, AsterixApiProviderPlugin.class, new AsterixLibraryProviderPlugin()); // TODO: no need to pass default instance
	}
	
	private static <T> void discoverAllPlugins(AsterixContext context, Class<T> type, T defaultProvider) {
		List<T> plugins = AsterixPluginDiscovery.discoverAllPlugins(type);
		if (plugins.isEmpty()) {
			log.debug("No plugin discovered for {}, using default {}", type.getName(), defaultProvider.getClass().getName());
			plugins.add(defaultProvider);
		}
		for (T plugin : plugins) {
			log.debug("Found plugin for {}, provider={}", type.getName(), plugin.getClass().getName());
			context.registerPlugin(type, plugin);
		}
	}

	private void configureVersioning(AsterixContext context) {
		if (enableVersioning) {
			discoverOnePlugin(context, AsterixVersioningPlugin.class);
		} else {
			context.registerPlugin(AsterixVersioningPlugin.class, AsterixVersioningPlugin.Default.create());
		}
	}

	private void configureFaultTolerance(AsterixContext context) {
		if (useFaultTolerance) {
			discoverOnePlugin(context, AsterixFaultTolerancePlugin.class);
		} else {
			context.registerPlugin(AsterixFaultTolerancePlugin.class, AsterixFaultTolerancePlugin.Default.create());
		}
	}
	
	private static <T> void discoverOnePlugin(AsterixContext context, Class<T> type) {
		T provider = AsterixPluginDiscovery.discoverOnePlugin(type);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
		context.registerPlugin(type, provider);
	}

	public <T> void registerDependency(T dependency) {
		this.externalDependencies.add(dependency);
	}

	public void registerDependency(ExternalDependencyBean externalDependency) {
		this.externalDependencyBeans.add(externalDependency);
	}
	
}
