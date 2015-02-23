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

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.publish.AstrixApiDescriptor;
import com.avanza.astrix.beans.publish.AstrixApiDescriptors;
import com.avanza.astrix.beans.publish.AstrixApiProviderPlugins;
import com.avanza.astrix.beans.service.StatefulAstrixBean;
import com.avanza.astrix.config.DynamicConfig;
/**
 * An AstrixContextImpl is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContextImpl providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixContextImpl implements Astrix, AstrixContext {
	
	private final SimpleAstrixFactoryBeanRegistry beanFactoryRegistry;
	private final AstrixApiProviderPlugins apiProviderPlugins;
	private final AstrixBeanFactory beanFactory;
	private final DynamicConfig dynamicConfig;
	private final AstrixInjector astrixInjector;
	
	
	public AstrixContextImpl(DynamicConfig dynamicConfig, AstrixInjector injector, AstrixApiProviderPlugins apiProviderPlugins) {
		this.dynamicConfig = dynamicConfig;
		this.astrixInjector = injector;
		this.apiProviderPlugins = apiProviderPlugins;
		this.beanFactory = this.astrixInjector.getBean(AstrixBeanFactory.class); // The bean-factory used for apis managed by astrix
		this.beanFactoryRegistry = (SimpleAstrixFactoryBeanRegistry) this.astrixInjector.getBean(AstrixFactoryBeanRegistry.class);
		for (AstrixApiDescriptor descriptor : this.astrixInjector.getBean(AstrixApiDescriptors.class).getAll()) {
			for (AstrixFactoryBean<?> factory : this.apiProviderPlugins.getProviderPlugin(descriptor).createFactoryBeans(descriptor)) {
				this.beanFactoryRegistry.registerFactory(factory);
			}
		}
	}
	
	
	<T> void registerBeanFactory(AstrixFactoryBean<T> beanFactory) {
		this.beanFactoryRegistry.registerFactory(beanFactory);
	}
	
	@Override
	public void destroy() {
		this.astrixInjector.destroy();
	}
	
	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return beanFactory.getBean(AstrixBeanKey.create(beanType, qualifier));
	}
	
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return beanFactory.getBean(beanKey);
	}

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		AstrixBeanKey<T> beanKey = AstrixBeanKey.create(beanType, qualifier);
		T bean = beanFactory.getBean(beanKey);
		for (AstrixBeanKey<?> dependencyKey : beanFactory.getDependencies(beanKey)) {
			Object dependency = beanFactory.getBean(dependencyKey);
			waitForBeanToBeBound(dependency, timeoutMillis);
		}
		waitForBeanToBeBound(bean, timeoutMillis);
		return bean;
	}
	private void waitForBeanToBeBound(Object bean, long timeoutMillis) throws InterruptedException {
		if (bean instanceof StatefulAstrixBean) {
			StatefulAstrixBean.class.cast(bean).waitUntilBound(timeoutMillis);
		}
	}

	public <T> T getPlugin(Class<T> pluginType) {
		return this.astrixInjector.getBean(pluginType);
	}
	
	/**
	 * Returns an instance of an internal framework class.
	 * @param classType
	 * @return
	 */
	public final <T> T getInstance(final Class<T> classType) {
		return this.astrixInjector.getBean(classType);
	}
	
	public DynamicConfig getConfig() {
		return dynamicConfig;
	}
	
	String getCurrentSubsystem() {
		return this.dynamicConfig.getStringProperty(AstrixSettings.SUBSYSTEM_NAME, null).get();
	}

}
