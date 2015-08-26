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
package com.avanza.astrix.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfigModule;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.ft.DefaultHystrixCommandNamingStrategy;
import com.avanza.astrix.beans.ft.FaultToleranceModule;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.ft.NoFaultTolerance;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviderPlugin;
import com.avanza.astrix.beans.publish.ApiProviderPlugins;
import com.avanza.astrix.beans.publish.ApiProviders;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.beans.publish.BeansPublishModule;
import com.avanza.astrix.beans.publish.PublishedBeanFactory;
import com.avanza.astrix.beans.registry.ServiceRegistryDiscoveryModule;
import com.avanza.astrix.beans.service.DirectComponentModule;
import com.avanza.astrix.beans.service.ServiceModule;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.config.PropertiesConfigSource;
import com.avanza.astrix.config.Setting;
import com.avanza.astrix.config.SystemPropertiesConfigSource;
import com.avanza.astrix.context.core.AstrixContextCoreModule;
import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.ModuleInstancePostProcessor;
import com.avanza.astrix.modules.Modules;
import com.avanza.astrix.modules.ModulesConfigurer;
import com.avanza.astrix.modules.NamedModule;
import com.avanza.astrix.modules.StrategyContextPreparer;
import com.avanza.astrix.modules.StrategyProvider;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixExcludedByProfile;
import com.avanza.astrix.provider.core.AstrixIncludedByProfile;
import com.avanza.astrix.versioning.core.ObjectSerializerModule;
import com.avanza.astrix.versioning.jackson1.Jackson1SerializerModule;
/**
 * Used to configure and create an {@link AstrixContext}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixConfigurer {

	private static final String CLASSPATH_OVERRIDE_SETTINGS = "META-INF/astrix/settings.properties";

	private static final Logger log = LoggerFactory.getLogger(AstrixConfigurer.class);
	
	private ApiProviders astrixApiProviders;
	private final Collection<StandardFactoryBean<?>> standaloneFactories = new LinkedList<>();
	private final List<Module> customModules = new ArrayList<>();
	private final Map<Class<?>, StrategyProvider<?>> strategyProviderByType = new HashMap<>();
	private final MapConfigSource settings = new MapConfigSource();
	
	private DynamicConfig customConfig = null;
	private final DynamicConfig wellKnownConfigSources = DynamicConfig.create(new SystemPropertiesConfigSource(), settings, PropertiesConfigSource.optionalClasspathPropertiesFile(CLASSPATH_OVERRIDE_SETTINGS));
	private final Set<String> activeProfiles = new HashSet<>();
	
	public AstrixConfigurer() {
	}
	
	/**
	 * Creates an AstrixContext instance using the current configuration. <p>
	 * 
	 * @return
	 */
	public AstrixContext configure() {
		
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.registerDefault(StrategyProvider.create(HystrixCommandNamingStrategy.class, DefaultHystrixCommandNamingStrategy.class));
		modulesConfigurer.registerDefault(StrategyProvider.create(FaultToleranceSpi.class, NoFaultTolerance.class));
		
		for (Module plugin : customModules) {
			modulesConfigurer.register(plugin);
		}
		
		loadAstrixContextPlugins(modulesConfigurer);
		
		for (StrategyProvider<?> strategyProvider : this.strategyProviderByType.values()) {
			modulesConfigurer.register(strategyProvider);
		}
		
		DynamicConfig config = createDynamicConfig();
		modulesConfigurer.register(new AstrixConfigModule(config, this.settings));
		modulesConfigurer.register(new DirectComponentModule());
		modulesConfigurer.register(new AstrixContextCoreModule());
		modulesConfigurer.register(new ServiceRegistryDiscoveryModule());
		modulesConfigurer.register(new ConfigDiscoveryModule());
		modulesConfigurer.register(new BeansPublishModule());
		modulesConfigurer.register(new ServiceModule());
		modulesConfigurer.register(new ObjectSerializerModule());
		modulesConfigurer.register(new Jackson1SerializerModule());
		modulesConfigurer.register(new GenericAstrixApiProviderModule());
		modulesConfigurer.register(new FaultToleranceModule());
		
		AstrixAwareInjector awareInjector = new AstrixAwareInjector(config);
		modulesConfigurer.registerBeanPostProcessor(awareInjector);
		Modules modules = modulesConfigurer.configure();
		awareInjector.setModules(modules);

		final AstrixContextImpl context = new AstrixContextImpl(modules);
		ApiProviders apiProviders = new FilteredApiProviders(getApiProviders(modules, config), activeProfiles);
		for (ApiProviderClass apiProvider : apiProviders.getAll()) {
			context.register(apiProvider);
		}
		
		// TODO: Merge with FilteredApiProviders and create module
		for (StandardFactoryBean<?> beanFactory : standaloneFactories) {
			log.debug("Registering standalone factory: bean={}", beanFactory.getBeanKey());
			context.registerBeanFactory(beanFactory);
		}
		return context;
	}

	private void loadAstrixContextPlugins(final ModulesConfigurer modulesConfigurer) {
		Iterator<AstrixContextPlugin> contextPlugins = ServiceLoader.load(AstrixContextPlugin.class).iterator();
		while (contextPlugins.hasNext()) {
			AstrixContextPlugin contextPlugin = contextPlugins.next();
			log.debug("Registering AstrixContextPlugin: astrixContextPlugin={}", contextPlugin.getClass().getName());
			contextPlugin.registerStrategies(new AstrixStrategiesConfig() {
				@Override
				public <T> void registerDefaultStrategy(Class<T> strategyType, Class<? extends T> strategyProvider) {
					modulesConfigurer.registerDefault(StrategyProvider.create(strategyType, strategyProvider));
				}
				@Override
				public <T> void registerStrategy(Class<T> strategyType,
						Class<? extends T> strategyImpl) {
					modulesConfigurer.register(StrategyProvider.create(strategyType, strategyImpl));
				}
				@Override
				public <T> void registerStrategy(Class<T> strategyType,
						Class<? extends T> strategyImpl,
						StrategyContextPreparer contextPreparer) {
					modulesConfigurer.register(StrategyProvider.create(strategyType, strategyImpl, contextPreparer));
				}
				
			});
			modulesConfigurer.register(contextPlugin);
		}
	}
	
	private DynamicConfig createDynamicConfig() {
		if (customConfig != null) {
			return DynamicConfig.merged(customConfig, wellKnownConfigSources);
		}
		String dynamicConfigFactoryClass = AstrixSettings.DYNAMIC_CONFIG_FACTORY.getFrom(wellKnownConfigSources).get();
		if (dynamicConfigFactoryClass != null) {
			AstrixDynamicConfigFactory dynamicConfigFactory = initFactory(dynamicConfigFactoryClass);
			DynamicConfig config = dynamicConfigFactory.create();
			return DynamicConfig.merged(config, wellKnownConfigSources);
		}
		return wellKnownConfigSources;
	}

	private AstrixDynamicConfigFactory initFactory(String dynamicConfigFactoryClass) {
		try {
			return (AstrixDynamicConfigFactory) Class.forName(dynamicConfigFactoryClass).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to init AstrixDynamicConfigFactoryClass: " + dynamicConfigFactoryClass, e);
		}
	}
	
	private final class AstrixAwareInjector implements ModuleInstancePostProcessor {
		private Modules modules;
		private DynamicConfig config;

		public AstrixAwareInjector(DynamicConfig config) {
			this.config = config;
		}

		public void setModules(Modules modules) {
			this.modules = modules;
		}
		
		@Override
		public void postProcess(Object bean) {
			if (bean instanceof AstrixPublishedBeansAware) {
				AstrixPublishedBeansAware.class.cast(bean).setAstrixBeans(new AstrixPublishedBeans() {
					@Override
					public <T> T getBean(AstrixBeanKey<T> beanKey) {
						return modules.getInstance(PublishedBeanFactory.class).getBean(beanKey);
					}
				});
			}
			if (bean instanceof AstrixConfigAware) {
				AstrixConfigAware.class.cast(bean).setConfig(config); // TODO: config
			}
		}
	}

	private static class FilteredApiProviders implements ApiProviders {
		
		private ApiProviders apiProviders;
		private Set<String> activeProfiles;
		
		public FilteredApiProviders(ApiProviders apiProviders, Set<String> activeProfiles) {
			this.apiProviders = apiProviders;
			this.activeProfiles = activeProfiles;
		}

		@Override
		public Collection<ApiProviderClass> getAll() {
			Set<ApiProviderClass> result = new HashSet<>();
			for (ApiProviderClass providerClass : apiProviders.getAll()) {
				if (isActive(providerClass)) {
					log.debug("Found provider: provider={}", providerClass.getProviderClassName());
					result.add(providerClass);
				}
			}
			return result;
		}
		
		private boolean isActive(ApiProviderClass providerClass) {
			if (providerClass.isAnnotationPresent(AstrixIncludedByProfile.class)) {
				AstrixIncludedByProfile activatedBy = providerClass.getAnnotation(AstrixIncludedByProfile.class);
				if (!this.activeProfiles.contains(activatedBy.value())) {
					log.debug("Rejecting provider, required profile not active. profile={} provider={}", activatedBy.value(), providerClass.getProviderClassName());
					return false;
				}
			}
			if (providerClass.isAnnotationPresent(AstrixExcludedByProfile.class)) {
				AstrixExcludedByProfile deactivatedBy = providerClass.getAnnotation(AstrixExcludedByProfile.class);
				if (this.activeProfiles.contains(deactivatedBy.value())) {
					log.debug("Rejecting provider, excluded by active profile. profile={} provider={}", deactivatedBy.value(), providerClass.getProviderClassName());
					return false;
				}
			}
			return true;
		}
	}

	private ApiProviders getApiProviders(Modules modules, DynamicConfig config) {
		if (this.astrixApiProviders != null) {
			return astrixApiProviders;
		}
		String basePackage = AstrixSettings.API_PROVIDER_SCANNER_BASE_PACKAGE.getFrom(config).get();
		if (!basePackage.trim().isEmpty()) {
			return new AstrixApiProviderClassScanner(getAllApiProviderAnnotationsTypes(modules), "com.avanza.astrix", basePackage.split(",")); // Always scan com.avanza.astrix package
		}
		return new AstrixApiProviderClassScanner(getAllApiProviderAnnotationsTypes(modules), "com.avanza.astrix"); 
	}
	
	private List<Class<? extends Annotation>> getAllApiProviderAnnotationsTypes(Modules modules) {
		List<Class<? extends Annotation>> result = new ArrayList<>();
		for (ApiProviderPlugin plugin : modules.getInstance(ApiProviderPlugins.class).getAll()) {
			result.add(plugin.getProviderAnnotationType());
		}
		return result;
	}
	
	/**
	 * Sets the base-package used when scanning for {@link AstrixApiProvider}'s.<p> 
	 * 
	 * @param basePackage
	 * @return 
	 */
	public AstrixConfigurer setBasePackage(String basePackage) {
		 this.settings.set(AstrixSettings.API_PROVIDER_SCANNER_BASE_PACKAGE, basePackage);
		 return this;
	}
	
	public AstrixConfigurer enableFaultTolerance(boolean enableFaultTolerance) {
		this.settings.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, enableFaultTolerance);
		return this;
	}
	
	// package private. Used for internal testing only
	void setAstrixApiProviders(ApiProviders astrixApiProviders) {
		this.astrixApiProviders = astrixApiProviders;
	}
	
	// package private. Used for internal testing only
	<T> AstrixConfigurer registerPlugin(final Class<T> type, final T provider) {
		customModules.add(new NamedModule() {
			@Override
			public void prepare(ModuleContext pluginContext) {
				pluginContext.bind(type, provider);
				pluginContext.export(type);
			}
			@Override
			public String name() {
				return "plugin-" + type.getName() + "[" + provider.getClass().getName() + "]";
			}
		});
		return this;
	}
	
	// package private. Used for internal testing only
	<T> void registerStrategy(final Class<T> strategyInterface, final T strategyInstance) {
		this.strategyProviderByType.put(strategyInterface, StrategyProvider.create(strategyInterface, strategyInstance));
	}

	public AstrixConfigurer set(String settingName, long value) {
		this.settings.set(settingName, Long.toString(value));
		return this;
	}
	
	public AstrixConfigurer set(String settingName, boolean value) {
		this.settings.set(settingName, Boolean.toString(value));
		return this;
	}

	public AstrixConfigurer set(String settingName, String value) {
		this.settings.set(settingName, value);
		return this;
	}
	
	public final <T> AstrixConfigurer set(Setting<T> setting, T value) {
		this.settings.set(setting, value);
		return this;
	}
	
	public final <T> AstrixConfigurer set(LongSetting setting, long value) {
		this.settings.set(setting, value);
		return this;
	}
	
	public AstrixConfigurer setSettings(Map<String, String> settings) {
		for (Map.Entry<String, String> setting : settings.entrySet()) {
			this.settings.set(setting.getKey(), setting.getValue());
		}
		return this;
	}
	
	public AstrixConfigurer setConfig(DynamicConfig config) {
		this.customConfig = config;
		return this;
	}
	
	/**
	 * Optional property that identifies what subsystem the current context belongs to. Its only
	 * allowed to invoke non-versioned services within the same subsystem. Attempting
	 * to invoke a non-versioned service in another subsystem will throw an IllegalSubsystemException. <p>
	 * 
	 * @param string
	 * @return 
	 */
	public AstrixConfigurer setSubsystem(String subsystem) {
		this.settings.set(AstrixSettings.SUBSYSTEM_NAME, subsystem);
		return this;
	}

	void addFactoryBean(StandardFactoryBean<?> factoryBean) {
		this.standaloneFactories.add(factoryBean);
	}

	void removeSetting(String name) {
		this.settings.set(name, null);
	}

	/**
	 * Activates a given Astrix profile.
	 * 
	 * Astrix profiles are used to include/exclude {@link AstrixApiProvider}'s at runtime by annotating them
	 * with {@link AstrixIncludedByProfile} and/or {@link AstrixExcludedByProfile}, typically to
	 * replace a given {@link AstrixApiProvider} in testing scenarios.<p>
	 * 
	 * 
	 * @param profile
	 * @return 
	 */
	public AstrixConfigurer activateProfile(String profile) {
		this.activeProfiles.add(profile);
		return this;
	}
	
	public void set(BooleanBeanSetting beanSetting, AstrixBeanKey<?> beanKey, boolean value) {
		set(beanSetting.nameFor(beanKey), value);
	}

	public void set(IntBeanSetting beanSetting, AstrixBeanKey<?> beanKey, int value) {
		set(beanSetting.nameFor(beanKey), value);
	}

	public void set(LongBeanSetting beanSetting, AstrixBeanKey<?> beanKey, long value) {
		set(beanSetting.nameFor(beanKey), value);
	}

}
