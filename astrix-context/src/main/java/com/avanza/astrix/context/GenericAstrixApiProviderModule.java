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

import java.util.Collections;
import java.util.Set;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.ClassConstructorFactoryBean;
import com.avanza.astrix.beans.publish.ApiProviderPlugin;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactoryPlugin;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;
import com.avanza.astrix.ft.BeanFaultToleranceProxyStrategy;

/**
 * 
 * @author Elias Lindholm
 *
 */
public class GenericAstrixApiProviderModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ApiProviderPlugin.class, GenericAstrixApiProviderPlugin.class);
		// TODO: should GenericAstrixApiProviderPlugin realy use AstrixInjector?
//		moduleContext.bind(AstrixFactoryBeanRegistry.class, new AstrixFactoryBeanRegistry() {
//			@Override
//			public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
//				return beanKey;
//			}
//			@Override
//			public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
//				return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
//			}
//			@Override
//			public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
//				return Collections.emptySet();
//			}
//		});
		
		moduleContext.importType(ServiceComponentRegistry.class);
		moduleContext.importType(AstrixConfig.class);
		moduleContext.importType(BeanFaultToleranceProxyStrategy.class);
		moduleContext.importType(ServiceDiscoveryMetaFactoryPlugin.class);
		
		moduleContext.export(ApiProviderPlugin.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}
	

}