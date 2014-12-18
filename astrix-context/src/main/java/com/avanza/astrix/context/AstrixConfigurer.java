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
package com.avanza.astrix.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AstrixConfigurer {


	private static final Logger log = LoggerFactory.getLogger(AstrixConfigurer.class);
	
	private AstrixApiDescriptors AstrixApiDescriptors;
	private final Collection<AstrixFactoryBean<?>> standaloneFactories = new LinkedList<>();
	private final List<PluginHolder<?>> plugins = new ArrayList<>();
	private final AstrixSettings settings = new AstrixSettings() {{
		set(SUBSYSTEM_NAME, "default");
	}};
	
	public AstrixContext configure() {
		AstrixContextImpl context = new AstrixContextImpl(settings);
		for (PluginHolder<?> plugin : plugins) {
			registerPlugin(context, plugin);
		}
		configureFaultTolerance(context);
		configureVersioning(context);
		discoverApiProviderPlugins(context);
		AstrixApiProviderPlugins apiProviderPlugins = new AstrixApiProviderPlugins(context.getPlugins(AstrixApiProviderPlugin.class));
		context.setApiProviderPlugins(apiProviderPlugins);
		AstrixApiProviderFactory apiProviderFactory = new AstrixApiProviderFactory(apiProviderPlugins);
		List<AstrixApiProvider> apiProviders = createApiProviders(apiProviderFactory, context);
		for (AstrixApiProvider apiProvider : apiProviders) {
			context.registerApiProvider(apiProvider);
		}
		for (AstrixFactoryBean<?> factoryBean : this.standaloneFactories) {
			context.registerBeanFactory(factoryBean);
		}
		return context;
	}
	
	private <T> void registerPlugin(AstrixContextImpl context, PluginHolder<T> pluginHolder) {
		context.registerPlugin(pluginHolder.pluginType, pluginHolder.pluginProvider);
	}

	private List<AstrixApiProvider> createApiProviders(AstrixApiProviderFactory apiProviderFactory, AstrixContextImpl context) {
		List<AstrixApiProvider> result = new ArrayList<>();
		for (AstrixApiDescriptor descriptor : getApiDescriptors(context).getAll()) {
			result.add(apiProviderFactory.create(descriptor));
		}
		return result;
	}

	private AstrixApiDescriptors getApiDescriptors(AstrixContextImpl context) {
		if (this.AstrixApiDescriptors != null) {
			return AstrixApiDescriptors;
		}
		String basePackage = context.getSettings().getString(AstrixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, "");
		if (basePackage.trim().isEmpty()) {
			return new AstrixApiDescriptorScanner(getAllDescriptorAnnotationsTypes(), "com.avanza.astrix"); // Always scan com.avanza.astrix package
		}
		return new AstrixApiDescriptorScanner(getAllDescriptorAnnotationsTypes(), "com.avanza.astrix", basePackage.split(","));
	}
	
	private List<Class<? extends Annotation>> getAllDescriptorAnnotationsTypes() {
		List<Class<? extends Annotation>> result = new ArrayList<>();
		for (AstrixApiProviderPlugin plugin : AstrixPluginDiscovery.discoverAllPlugins(AstrixApiProviderPlugin.class)) {
			result.add(plugin.getProviderAnnotationType());
		}
		return result;
	}

	public void setBasePackage(String basePackage) {
		 this.settings.set(AstrixSettings.API_DESCRIPTOR_SCANNER_BASE_PACKAGE, basePackage);
	}
	
	public void enableFaultTolerance(boolean enableFaultTolerance) {
		this.settings.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, enableFaultTolerance);
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.settings.set(AstrixSettings.ENABLE_VERSIONING, enableVersioning);
	}
	
	private void discoverApiProviderPlugins(AstrixContextImpl context) {
		discoverAllPlugins(context, AstrixApiProviderPlugin.class);
	}
	
	private static <T> void discoverAllPlugins(AstrixContextImpl context, Class<T> type) {
		List<T> plugins = AstrixPluginDiscovery.discoverAllPlugins(type);
		if (plugins.isEmpty()) {
			log.debug("No plugin discovered for {}", type.getName());
		}
		for (T plugin : plugins) {
			log.debug("Found plugin for {}, provider={}", type.getName(), plugin.getClass().getName());
			context.registerPlugin(type, plugin);
		}
	}

	private void configureVersioning(AstrixContextImpl context) {
		if (context.getSettings().getBoolean(AstrixSettings.ENABLE_VERSIONING, true)) {
			discoverOnePlugin(context, AstrixVersioningPlugin.class);
		} else {
			context.registerPlugin(AstrixVersioningPlugin.class, AstrixVersioningPlugin.Default.create());
		}
	}
	
	private void configureFaultTolerance(AstrixContextImpl context) {
		if (context.getSettings().getBoolean(AstrixSettings.ENABLE_FAULT_TOLERANCE, false)) {
			discoverOnePlugin(context, AstrixFaultTolerancePlugin.class);
		} else {
			context.registerPlugin(AstrixFaultTolerancePlugin.class, AstrixFaultTolerancePlugin.Default.create());
		}
	}
	
	private static <T> void discoverOnePlugin(AstrixContextImpl context, Class<T> type) {
		T provider = context.getPlugin(type);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
	}

	// package private. Used for internal testing only
	void setAstrixApiDescriptors(AstrixApiDescriptors AstrixApiDescriptors) {
		this.AstrixApiDescriptors = AstrixApiDescriptors;
	}
	
	// package private. Used for internal testing only
	<T> void registerPlugin(Class<T> c, T provider) {
		plugins.add(new PluginHolder<>(c, provider));
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
	
	public void setAstrixSettings(AstrixSettings settings) {
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
	 * to invoke a non-versioned service in another subsystem will throw an IllegalSubsystemException. <p>
	 * 
	 * @param string
	 */
	public void setSubsystem(String subsystem) {
		this.settings.set(AstrixSettings.SUBSYSTEM_NAME, subsystem);
	}

	public void addFactoryBean(AstrixFactoryBean<?> factoryBean) {
		this.standaloneFactories.add(factoryBean);
	}

}
