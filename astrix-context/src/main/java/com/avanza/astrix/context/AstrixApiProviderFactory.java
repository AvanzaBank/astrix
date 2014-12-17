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

import java.util.ArrayList;
import java.util.List;


/**
 * This component is used to create runtime factory representations (AstrixApiProvider) for api's hooked
 * into Astrix. An API is defined by an AstrixApiDescriptor, which in turn uses different annotations for
 * different types of apis. This class is responsible for interpreting such annotations and create an
 * AstrixApiProvider for the given api. <p>
 * 
 * The factory is extendible by adding more {@link AstrixApiProviderPlugin}'s. <p>
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApiProviderFactory {
	
	private final AstrixApiProviderPlugins apiProviderPlugins;
	
	public AstrixApiProviderFactory(AstrixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}
	
	public AstrixApiProvider create(AstrixApiDescriptor descriptor) {
		AstrixApiProviderPlugin providerFactoryPlugin = getProviderPlugin(descriptor);
		List<AstrixFactoryBean<?>> factoryBeans = new ArrayList<>();
		for (AstrixFactoryBeanPlugin<?> factoryBean : providerFactoryPlugin.createFactoryBeans(descriptor)) {
			if (factoryBean.isStateful()) {
				factoryBeans.add(AstrixFactoryBean.stateful(factoryBean, descriptor));
			} else {
				factoryBeans.add(AstrixFactoryBean.nonStateful(factoryBean, descriptor));
			}
		}
		return new AstrixApiProvider(factoryBeans, descriptor); 
	}
	
	private AstrixApiProviderPlugin getProviderPlugin(AstrixApiDescriptor descriptor) {
		return this.apiProviderPlugins.getProviderPlugin(descriptor);
	}
	
}
