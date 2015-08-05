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

import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.factory.BeanConfigurationsImpl;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.NamedModule;

public class BeansPublishModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(PublishedBeanFactory.class, PublishedBeanFactoryImpl.class);
		moduleContext.bind(BeanPublisher.class, BeanPublisherImpl.class);
		moduleContext.bind(ApiProviderPlugins.class, ApiProviderPluginsImpl.class);
		
		moduleContext.bind(AstrixFactoryBeanRegistry.class, SimpleAstrixFactoryBeanRegistry.class);
		moduleContext.bind(BeanConfigurations.class, BeanConfigurationsImpl.class);
		
		moduleContext.importType(ApiProviderPlugin.class);
		
		moduleContext.export(PublishedBeanFactory.class);
		moduleContext.export(AstrixPublishedBeans.class);
		moduleContext.export(ApiProviderPlugins.class);
		moduleContext.export(BeanPublisher.class);
		moduleContext.export(BeanConfigurations.class); // TODO; should BeanConfigurations be exported from this module?
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
