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
package com.avanza.asterix.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AsterixConfigurer {


	private static final Logger log = LoggerFactory.getLogger(AsterixConfigurer.class);
	
	private AsterixApiDescriptors asterixApiDescriptors;
	private final Collection<AsterixFactoryBean<?>> standaloneFactories = new LinkedList<>();
	private final List<PluginHolder<?>> plugins = new ArrayList<>();
	private boolean enableFaultTolerance = false;
	private boolean enableVersioning = true;
	private boolean enableMonitoring = true; 
	private final AsterixSettings settings = new AsterixSettings();
	private String subsystem = "unknown";
	
	public AsterixContext configure() {
		AsterixContext context = new AsterixContext(settings, subsystem);
		for (PluginHolder<?> plugin : plugins) {
			registerPlugin(context, plugin);
		}
		configureFaultTolerance(context);
		configureVersioning(context);
		configureMonitoring(context);
		discoverApiProviderPlugins(context);
		AsterixApiProviderPlugins apiProviderPlugins = new AsterixApiProviderPlugins(context.getPlugins(AsterixApiProviderPlugin.class));
		context.setApiProviderPlugins(apiProviderPlugins);
		AsterixApiProviderFactory apiProviderFactory = new AsterixApiProviderFactory(apiProviderPlugins);
		List<AsterixApiProvider> apiProviders = createApiProviders(apiProviderFactory, context);
		for (AsterixApiProvider apiProvider : apiProviders) {
			context.registerApiProvider(apiProvider);
		}
		for (AsterixFactoryBean<?> factoryBean : this.standaloneFactories) {
			context.registerBeanFactory(factoryBean);
		}
		return context;
	}
	
	private <T> void registerPlugin(AsterixContext context, PluginHolder<T> pluginHolder) {
		context.registerPlugin(pluginHolder.pluginType, pluginHolder.pluginProvider);
	}

	private List<AsterixApiProvider> createApiProviders(AsterixApiProviderFactory apiProviderFactory, AsterixContext context) {
		List<AsterixApiProvider> result = new ArrayList<>();
		for (AsterixApiDescriptor descriptor : getApiDescriptors(context).getAll()) {
			result.add(apiProviderFactory.create(descriptor));
		}
		return result;
	}

	private AsterixApiDescriptors getApiDescriptors(AsterixContext context) {
		if (this.asterixApiDescriptors != null) {
			return asterixApiDescriptors;
		}
		String basePackage = context.getSettings().getString(AsterixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, "");
		if (basePackage.trim().isEmpty()) {
			return new AsterixApiDescriptorScanner();
		}
		return new AsterixApiDescriptorScanner(basePackage.split(","));
	}
	
	public void setBasePackage(String basePackage) {
		 this.settings.set(AsterixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, basePackage);
	}
	
	public void enableFaultTolerance(boolean enableFaultTolerance) {
		this.enableFaultTolerance  = enableFaultTolerance;
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.enableVersioning = enableVersioning;
	}
	
	public void enableMonitoring(boolean enableMonitoring) {
		this.enableMonitoring = enableMonitoring;
	}

	private void discoverApiProviderPlugins(AsterixContext context) {
		discoverAllPlugins(context, AsterixApiProviderPlugin.class);
	}
	
	private static <T> void discoverAllPlugins(AsterixContext context, Class<T> type) {
		List<T> plugins = AsterixPluginDiscovery.discoverAllPlugins(type);
		if (plugins.isEmpty()) {
			log.debug("No plugin discovered for {}", type.getName());
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
	
	private void configureMonitoring(AsterixContext context) {
		if (enableMonitoring) {
			MetricsPoller metricsPoller = context.newInstance(MetricsPoller.class);
			metricsPoller.start();
		}
	}

	private void configureFaultTolerance(AsterixContext context) {
		if (enableFaultTolerance) {
			discoverOnePlugin(context, AsterixFaultTolerancePlugin.class);
		} else {
			context.registerPlugin(AsterixFaultTolerancePlugin.class, AsterixFaultTolerancePlugin.Default.create());
		}
	}
	
	private static <T> T discoverOnePlugin(AsterixContext context, Class<T> type) {
		T provider = AsterixPluginDiscovery.discoverOnePlugin(type);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
		context.registerPlugin(type, provider);
		return provider;
	}

	// package private. Used for internal testing only
	void setAsterixApiDescriptors(AsterixApiDescriptors asterixApiDescriptors) {
		this.asterixApiDescriptors = asterixApiDescriptors;
	}
	
	// package private. Used for internal testing only
	<T> void registerPlugin(Class<T> c, T provider) {
		plugins .add(new PluginHolder<>(c, provider));
	}

	public void set(String settingName, long value) {
		this.settings.set(settingName, value);
	}
	
	public void set(String settingName, boolean value) {
		this.settings.set(settingName, value);
	}
	
	public void set(String settingName, String value) {
		this.settings.set(settingName, value);
	}
	
	public void setSettings(Map<String, String> settings) {
		this.settings.setAll(settings);
	}
	
	@Autowired(required = false)
	public void setAsterixSettings(AsterixSettings settings) {
		this.settings.setAll(settings);
	}
	
	private static class PluginHolder<T> {
		private Class<T> pluginType;
		private T pluginProvider;
		public PluginHolder(Class<T> pluginType, T pluginProvider) {
			this.pluginType = pluginType;
			this.pluginProvider = pluginProvider;
		}
	}

	/**
	 * Optional property that identifies what subsystem the current context belongs to. Its only
	 * allowed to invoke non-versioned services within the same subsystem. Attempting
	 * to create an bean in another subsystem will throw an exception. <p>
	 * 
	 * @param string
	 */
	public void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}

	public void addFactoryBean(AsterixFactoryBean<?> factoryBean) {
		this.standaloneFactories.add(factoryBean);
	}

}
