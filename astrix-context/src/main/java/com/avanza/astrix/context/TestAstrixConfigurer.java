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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviders;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.Setting;

public class TestAstrixConfigurer {
	
	private AstrixConfigurer configurer;
	private final Set<ApiProviderClass> apiProviders = new HashSet<>();
	private final Collection<StandardFactoryBean<?>> standaloneFactories = new LinkedList<>();
	
	public TestAstrixConfigurer() {
		configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return apiProviders;
			}
		});
		configurer.enableVersioning(false);
		configurer.enableFaultTolerance(false);
		configurer.set(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, false);
	}

	public AstrixContext configure() {
		for (StandardFactoryBean<?> factoryBean : standaloneFactories) {
			configurer.addFactoryBean(factoryBean);
		}
		return (AstrixContextImpl) configurer.configure();
	}

	/**
	 * @deprecated renamed to registerApiProvider
	 * @param descriptor
	 */
	@Deprecated
	public void registerApiDescriptor(Class<?> descriptor) {
		registerApiProvider(descriptor);
	}
	
	/**
	 * Registers api provider(s).
	 * 
	 * This is an alias for registerApiDescriptor(Class<?> descriptor), which
	 * should be removed in the future.
	 * 
	 * @param provider
	 */
	public void registerApiProvider(Class<?>... providers) {
		for (Class<?> provider : providers) {
			apiProviders.add(ApiProviderClass.create(provider));
		}
	}

	public <T> void registerAstrixBean(Class<T> beanType, T provider) {
		registerAstrixBean(beanType, provider, null);
	}
	
	public <T> void registerAstrixBean(Class<T> beanType, T provider, String qualifier) {
		StandaloneFactoryBean<T> factoryBean = new StandaloneFactoryBean<>(beanType, provider, qualifier);
		standaloneFactories.add(factoryBean);
	}

	public <T> void registerPlugin(Class<T> c, T provider) {
		configurer.registerPlugin(c, provider);
	}
	
	public void enableFaultTolerance(boolean enabled) {
		configurer.enableFaultTolerance(enabled);
	}

	public void set(String name, long value) {
		configurer.set(name, value);
	}
	
	public void set(String name, String value) {
		configurer.set(name, value);
	}

	public void set(String name, boolean value) {
		configurer.set(name, value);
	}

	public final <T> void set(Setting<T> setting, T value) {
		this.configurer.set(setting, value);
	}
	
	public final void set(LongSetting setting, long value) {
		this.configurer.set(setting, value);
	}
	
	/*
	 * Sets the current subsystem name and enables enforcing of
	 * subsystem boundaries
	 */
	public void setSubsystem(String subsystem) {
		this.configurer.setSubsystem(subsystem);
		this.configurer.set(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
	}
	
	private static class StandaloneFactoryBean<T> implements StandardFactoryBean<T> {
		private AstrixBeanKey<T> type;
		private T instance;

		public StandaloneFactoryBean(Class<T> type, T instance, String qualifier) {
			this.type = AstrixBeanKey.create(type, qualifier);
			this.instance = instance;
		}

		@Override
		public T create(AstrixBeans beans) {
			return instance;
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return type;
		}
		
	}

	public TestAstrixConfigurer enableVersioning(boolean enableVersioning) {
		this.configurer.enableVersioning(enableVersioning);
		return this;
	}

	public void removeSetting(String name) {
		this.configurer.removeSetting(name);
	}

	public void activateProfile(String profile) {
		this.configurer.activateProfile(profile);
	}

}
