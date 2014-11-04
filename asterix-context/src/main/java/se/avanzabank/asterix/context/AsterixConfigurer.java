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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AsterixConfigurer {


	private static final Logger log = LoggerFactory.getLogger(AsterixConfigurer.class);
	
	private AsterixApiDescriptors asterixApiDescriptors = new AsterixApiDescriptorScanner("se.avanzabank");
	private boolean enableFaultTolerance = false;
	private boolean enableVersioning = true;
	private boolean enableMonitoring = true; 
	private List<ExternalDependencyBean> externalDependencyBeans = new ArrayList<>();
	private List<Object> externalDependencies = new ArrayList<>();
	private final AsterixSettings settings = new AsterixSettings();
	private final List<PluginHolder<?>> plugins = new ArrayList<>();
	private String subsystem = "unknown";
	
	public AsterixContext configure() {
		AsterixContext context = new AsterixContext(settings, subsystem);
		for (PluginHolder<?> plugin : plugins) {
			registerPlugin(context, plugin);
		}
		context.setExternalDependencyBeans(externalDependencyBeans);
		context.setExternalDependencies(externalDependencies);
		configureFaultTolerance(context);
		configureVersioning(context);
		configureMonitoring(context);
		discoverApiProviderPlugins(context);
		AsterixApiProviderPlugins apiProviderPlugins = new AsterixApiProviderPlugins(context.getPlugins(AsterixApiProviderPlugin.class));
		context.setApiProviderPlugins(apiProviderPlugins);
		AsterixApiProviderFactory apiProviderFactory = new AsterixApiProviderFactory(apiProviderPlugins);
		List<AsterixApiProvider> apiProviders = createApiProviders(apiProviderFactory);
		for (AsterixApiProvider apiProvider : apiProviders) {
			context.registerApiProvider(apiProvider);
		}
		return context;
	}
	
	private <T> void registerPlugin(AsterixContext context, PluginHolder<T> pluginHolder) {
		context.registerPlugin(pluginHolder.pluginType, pluginHolder.pluginProvider);
	}

	private List<AsterixApiProvider> createApiProviders(AsterixApiProviderFactory apiProviderFactory) {
		List<AsterixApiProvider> result = new ArrayList<>();
		for (AsterixApiDescriptor descriptor : asterixApiDescriptors.getAll()) {
			result.add(apiProviderFactory.create(descriptor));
		}
		return result;
	}

	@Autowired(required = false)
	public void setExternalDependencies(List<ExternalDependencyBean> externalDependencies) {
		this.externalDependencyBeans = externalDependencies;
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
	
	private void configureMonitoring(AsterixContext context) {
		// TODO stop poller
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

	public <T> void registerDependency(T dependency) {
		this.externalDependencies.add(dependency);
	}

	public void registerDependency(ExternalDependencyBean externalDependency) {
		this.externalDependencyBeans.add(externalDependency);
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

}
