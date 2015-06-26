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
package com.avanza.astrix.beans.publish;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.beans.factory.BeanConfigurationsImpl;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class PublishedBeanFactoryImpl implements PublishedBeanFactory, AstrixPublishedBeans {

	private static final Logger log = LoggerFactory.getLogger(PublishedBeanFactoryImpl.class);
	
	private final SimpleAstrixFactoryBeanRegistry beanFactoryRegistry;
	private final AstrixBeanFactory beanFactory;
	private final BeanConfigurationsImpl beanConfigurations;
	
	public PublishedBeanFactoryImpl(SimpleAstrixFactoryBeanRegistry beanFactoryRegistry,
									AstrixBeanFactory beanFactory,
									BeanConfigurationsImpl beanConfigurations) {
		this.beanFactoryRegistry = beanFactoryRegistry;
		this.beanFactory = beanFactory;
		this.beanConfigurations = beanConfigurations;
	}

//	@Override
//	public void register(ApiProviderClass apiProvider) {
//		log.debug("Registering apiProvider={}", apiProvider.getName());
//		for (PublishedBean publishedBean : createPublishedBeans(apiProvider)) {
//			log.debug("Registering factory for bean. beanKey={} apiProvider={} factoryType={}",  
//					publishedBean.getBeanKey(), 
//					apiProvider.getName(), 
//					publishedBean.getFactory().getClass().getName());
//			this.beanFactoryRegistry.registerFactory(publishedBean.getFactory());
//			this.beanConfigurations.setDefaultBeanConfig(publishedBean.getBeanKey(), publishedBean.getDefaultBeanSettingsOverride());
//		}
//	}
	
//	private Collection<PublishedBean> createPublishedBeans(ApiProviderClass apiProvider) {
//		return apiProviderPlugins.getProviderPlugin(apiProvider).createFactoryBeans(apiProvider);
//	}

	@Override
	public BeanConfiguration getBeanConfiguration(AstrixBeanKey<?> beanKey) {
		return this.beanConfigurations.getBeanConfiguration(beanKey);
	}

	@Override
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return this.beanFactory.getBean(beanKey);
	}

//	@Override
//	public <T> void registerFactoryBean(StandardFactoryBean<T> beanFactory) {
//		this.beanFactoryRegistry.registerFactory(beanFactory);
//	}

	@Override
	public Set<AstrixBeanKey<? extends Object>> getDependencies(AstrixBeanKey<? extends Object> beanKey) {
		return this.beanFactory.getDependencies(beanKey);
	}
	
	public void registerBean(PublishedBean publishedBean) {
		this.beanFactoryRegistry.registerFactory(publishedBean.getFactory());
		this.beanConfigurations.setDefaultBeanConfig(publishedBean.getBeanKey(), publishedBean.getDefaultBeanSettingsOverride());
	}
}
