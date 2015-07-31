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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.BeanPublisher;
import com.avanza.astrix.beans.publish.PublishedBeanFactory;
import com.avanza.astrix.beans.service.StatefulAstrixBean;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.module.ModuleManager;
/**
 * An AstrixContextImpl is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContextImpl providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
final class AstrixContextImpl implements Astrix, AstrixApplicationContext {
	
	private final PublishedBeanFactory publishedBeanFactory;
	private final BeanPublisher beanPublisher;
	private final DynamicConfig dynamicConfig;
	private final ModuleManager moduleManager;
	
	public AstrixContextImpl(DynamicConfig config, ModuleManager moduleManager) {
		this.dynamicConfig = config;
		this.moduleManager = moduleManager;
		this.beanPublisher = moduleManager.getInstance(BeanPublisher.class);
		this.publishedBeanFactory = moduleManager.getInstance(PublishedBeanFactory.class);
	}
	

	void register(ApiProviderClass apiProvider) {
		this.beanPublisher.register(apiProvider);		
	}

	<T> void registerBeanFactory(StandardFactoryBean<T> beanFactory) {
		this.beanPublisher.registerFactoryBean(beanFactory);
	}
	
	@Override
	public void destroy() {
		this.moduleManager.destroy();
	}

	@Override
	public void close() throws Exception {
		destroy();
	}
	
	public BeanConfiguration getBeanConfiguration(AstrixBeanKey<?> beanKey) {
		return this.publishedBeanFactory.getBeanConfiguration(beanKey);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return publishedBeanFactory.getBean(AstrixBeanKey.create(beanType, qualifier));
	}
	
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return publishedBeanFactory.getBean(beanKey);
	}
	

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		AstrixBeanKey<T> beanKey = AstrixBeanKey.create(beanType, qualifier);
		T bean = publishedBeanFactory.getBean(beanKey);
		for (AstrixBeanKey<?> dependencyKey : publishedBeanFactory.getDependencies(beanKey)) {
			Object dependency = publishedBeanFactory.getBean(dependencyKey);
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

	/**
	 * Returns an instance of an internal framework class.
	 * @param classType
	 * @return
	 */
	public final <T> T getInstance(final Class<T> classType) {
		return this.moduleManager.getInstance(classType);
	}
	
	@Override
	public DynamicConfig getConfig() {
		return dynamicConfig;
	}
	
}
