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

public class TestAstrixConfigurer {
	
	private AstrixConfigurer configurer;
	private final Set<AstrixApiDescriptor> descriptors = new HashSet<>();
	private final Collection<AstrixFactoryBean<?>> standaloneFactories = new LinkedList<>();
	
	public TestAstrixConfigurer() {
		configurer = new AstrixConfigurer();
		configurer.setAstrixApiDescriptors(new AstrixApiDescriptors() {
			@Override
			public Collection<AstrixApiDescriptor> getAll() {
				return descriptors;
			}
		});
		configurer.enableVersioning(false);
		configurer.enableFaultTolerance(false);
		configurer.set(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, false);
	}

	public AstrixContext configure() {
		for (AstrixFactoryBean<?> factoryBean : standaloneFactories) {
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
	 * Registers an api provider.
	 * 
	 * This is an alias for registerApiDescriptor(Class<?> descriptor), which
	 * should be removed in the future.
	 * 
	 * @param provider
	 */
	public void registerApiProvider(Class<?> provider) {
		descriptors.add(AstrixApiDescriptor.create(provider));
	}
	

	public <T> void registerAstrixBean(Class<T> beanType, T provider) {
		StandaloneFactoryBean<T> factoryPlugin = new StandaloneFactoryBean<>(beanType, provider);
		AstrixApiDescriptor apiDescriptor = AstrixApiDescriptor.simple(provider.getClass().getName());
		AstrixFactoryBean<T> factoryBean = AstrixFactoryBean.nonStateful(factoryPlugin, apiDescriptor);
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
	
	/*
	 * Sets the current subsystem name and enables enforcing of
	 * subsystem boundaries
	 */
	public void setSubsystem(String subsystem) {
		this.configurer.setSubsystem(subsystem);
		this.configurer.set(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
	}
	
	private static class StandaloneFactoryBean<T> implements AstrixFactoryBeanPlugin<T> {
		private Class<T> type;
		private T instance;

		public StandaloneFactoryBean(Class<T> type, T instance) {
			this.type = type;
			this.instance = instance;
		}

		@Override
		public T create(String optionalQualifier) {
			return instance;
		}

		@Override
		public Class<T> getBeanType() {
			return type;
		}
		
	}
	
}
