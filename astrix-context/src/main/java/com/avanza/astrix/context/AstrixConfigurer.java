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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.inject.AstrixPlugins;
import com.avanza.astrix.beans.inject.AstrixStrategies;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviderPlugin;
import com.avanza.astrix.beans.publish.ApiProviders;
import com.avanza.astrix.beans.publish.AstrixConfigAware;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.beans.service.AstrixVersioningPlugin;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.PropertiesConfigSource;
import com.avanza.astrix.config.Setting;
import com.avanza.astrix.config.SystemPropertiesConfigSource;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixExcludedByProfile;
import com.avanza.astrix.provider.core.AstrixIncludedByProfile;
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
	private final List<AstrixPlugins.Plugin<?>> plugins = new ArrayList<>();
	private final AstrixSettings settings = new AstrixSettings();
	
	private DynamicConfig customConfig = null;
	private final DynamicConfig wellKnownConfigSources = DynamicConfig.create(new SystemPropertiesConfigSource(), settings, PropertiesConfigSource.optionalClasspathPropertiesFile(CLASSPATH_OVERRIDE_SETTINGS));
	private final Set<String> activeProfiles = new HashSet<>();
	private DynamicConfig config;
	
	/**
	 * Creates an AstrixContext instance using the current configuration. <p>
	 * 
	 * @return
	 */
	public AstrixContext configure() {
		config = createDynamicConfig();
		AstrixPlugins astrixPlugins = getPlugins();
		AstrixStrategies astrixStrategies = new AstrixStrategies(config);
		AstrixInjector injector = new AstrixInjector(astrixPlugins, astrixStrategies);
		injector.bind(DynamicConfig.class, config);
		injector.bind(AstrixContext.class, AstrixContextImpl.class);
		injector.bind(AstrixFactoryBeanRegistry.class, SimpleAstrixFactoryBeanRegistry.class);
		injector.bind(ApiProviders.class, new FilteredApiProviders(getApiProviders(astrixPlugins), activeProfiles));
		injector.registerBeanPostProcessor(new InternalBeanPostProcessor(injector.getBean(AstrixBeanFactory.class)));
		AstrixContextImpl context = injector.getBean(AstrixContextImpl.class);
		for (StandardFactoryBean<?> beanFactory : standaloneFactories) {
			log.debug("Registering standalone factory: bean={}", beanFactory.getBeanKey());
			context.registerBeanFactory(beanFactory);
		}
		return context;
	}
	
	private AstrixPlugins getPlugins() {
		AstrixPlugins result = new AstrixPlugins();
		for (AstrixPlugins.Plugin<?> plugin : plugins) {
			result.registerPlugin(plugin);
		}
		configureVersioning(result);
		return result;
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
	
	private static class FilteredApiProviders implements ApiProviders {
		
		private ApiProviders apiProviders;
		private Set<String> activeProfiles;
		
		public FilteredApiProviders(ApiProviders apiProviders, Set<String> activeProfiles) {
			this.apiProviders = apiProviders;
			this.activeProfiles = activeProfiles;
		}

		@Override
		public Collection<ApiProviderClass> getAll() {
			List<ApiProviderClass> result = new LinkedList<>();
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

	private ApiProviders getApiProviders(AstrixPlugins astrixPlugins) {
		if (this.astrixApiProviders != null) {
			return astrixApiProviders;
		}
		String basePackage = AstrixSettings.API_PROVIDER_SCANNER_BASE_PACKAGE.getFrom(config).get();
		if (!basePackage.trim().isEmpty()) {
			return new AstrixApiProviderClassScanner(getAllApiProviderAnnotationsTypes(astrixPlugins), "com.avanza.astrix", basePackage.split(",")); // Always scan com.avanza.astrix package
		}
		return new AstrixApiProviderClassScanner(getAllApiProviderAnnotationsTypes(astrixPlugins), "com.avanza.astrix"); 
	}
	
	private List<Class<? extends Annotation>> getAllApiProviderAnnotationsTypes(AstrixPlugins astrixPlugins) {
		List<Class<? extends Annotation>> result = new ArrayList<>();
		for (ApiProviderPlugin plugin : astrixPlugins.getPlugins(ApiProviderPlugin.class)) {
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
	
	public AstrixConfigurer enableVersioning(boolean enableVersioning) {
		this.settings.set(AstrixSettings.ENABLE_VERSIONING, enableVersioning);
		return this;
	}
	
	private void configureVersioning(AstrixPlugins plugins) {
		if (AstrixSettings.ENABLE_VERSIONING.getFrom(config).get()) {
			discoverOnePlugin(plugins, AstrixVersioningPlugin.class);
		} else {
			plugins.registerPlugin(AstrixVersioningPlugin.class, AstrixVersioningPlugin.Default.create());
		}
	}
	
	private static <T> void discoverOnePlugin(AstrixPlugins plugin, Class<T> type) {
		T provider = plugin.getPlugin(type);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
	}

	// package private. Used for internal testing only
	void setAstrixApiProviders(ApiProviders astrixApiProviders) {
		this.astrixApiProviders = astrixApiProviders;
	}
	
	// package private. Used for internal testing only
	<T> AstrixConfigurer registerPlugin(Class<T> c, T provider) {
		plugins.add(new AstrixPlugins.Plugin<>(c, Arrays.asList(provider)));
		return this;
	}

	public AstrixConfigurer set(String settingName, long value) {
		this.settings.set(settingName, value);
		return this;
	}
	
	public AstrixConfigurer set(String settingName, boolean value) {
		this.settings.set(settingName, value);
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
		this.settings.setAll(settings);
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
		this.settings.remove(name);
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
	
	public static  class InternalBeanPostProcessor implements AstrixBeanPostProcessor {
		
		private final AstrixBeanFactory publishedApis;
		
		public InternalBeanPostProcessor(AstrixBeanFactory publishedApis) {
			this.publishedApis = publishedApis;
		}

		@Override
		public void postProcess(Object bean, AstrixBeans beans) {
			injectAwareDependencies(bean, beans);
		}
		
		private void injectAwareDependencies(Object object, AstrixBeans beans) {
			if (object instanceof AstrixPublishedBeansAware) {
				injectBeanDependencies((AstrixPublishedBeansAware)object);
			}
			if (object instanceof AstrixConfigAware) {
				AstrixConfigAware.class.cast(object).setConfig(beans.getBean(AstrixBeanKey.create(DynamicConfig.class)));
			}
		}
		
		private void injectBeanDependencies(AstrixPublishedBeansAware beanDependenciesAware) {
			beanDependenciesAware.setAstrixBeans(new AstrixPublishedBeans() {
				@Override
				public <T> T getBean(AstrixBeanKey<T> beanKey) {
					return publishedApis.getBean(beanKey);
				}
			});
		}
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
