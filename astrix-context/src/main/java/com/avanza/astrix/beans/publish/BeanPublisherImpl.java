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

import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.factory.StandardFactoryBean;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanPublisherImpl implements BeanPublisher {

	private static final Logger log = LoggerFactory.getLogger(BeanPublisherImpl.class);

	private final ApiProviderPluginsImpl apiProviderPluginsImpl;
	private final PublishedBeanFactoryImpl beanFactory;

	public BeanPublisherImpl(ApiProviderPluginsImpl apiProviderPluginsImpl, PublishedBeanFactoryImpl beanFactory) {
		this.apiProviderPluginsImpl = apiProviderPluginsImpl;
		this.beanFactory = beanFactory;
	}

	@Override
	public void register(ApiProviderClass apiProvider) {
		log.debug("Registering apiProvider={}", apiProvider.getName());
		for (PublishedBean publishedBean : createPublishedBeans(apiProvider)) {
			log.debug("Registering factory for bean. beanKey={} apiProvider={} factoryType={}",  
					publishedBean.getBeanKey(), 
					apiProvider.getName(), 
					publishedBean.getFactory().getClass().getName());
			this.beanFactory.registerBean(publishedBean);
		}
	}
	
	private Collection<PublishedBean> createPublishedBeans(ApiProviderClass apiProvider) {
		return apiProviderPluginsImpl.getProviderPlugin(apiProvider).createFactoryBeans(apiProvider);
	}

	@Override
	public <T> void registerFactoryBean(StandardFactoryBean<T> beanFactory) {
		this.beanFactory.registerBean(new PublishedBean(beanFactory, new HashMap<AstrixBeanSettings.BeanSetting<?>, Object>()));
	}
}
