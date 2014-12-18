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

import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceBeanDefinition {
	
	private AstrixBeanKey<?> beanKey;
	private AstrixApiDescriptor apiDescriptor;
	private AstrixApiProviderPlugin apiProvider;
	private String componentName;
	private boolean usesServiceRegistry;
	

	public AstrixServiceBeanDefinition(AstrixBeanKey<?> beanKey,
									   AstrixApiDescriptor apiDescriptor,
									   AstrixApiProviderPlugin apiProvider,
									   boolean usesServiceRegistry,
									   String componentName) {
		super();
		this.beanKey = beanKey;
		this.apiDescriptor = apiDescriptor;
		this.apiProvider = apiProvider;
		this.usesServiceRegistry = usesServiceRegistry;
		this.componentName = componentName;
	}

	public AstrixBeanKey<?> getBeanKey() {
		return this.beanKey;
	}
	
	public String getComponentName() {
		return componentName;
	}

	public ServiceVersioningContext createVersioningContext() {
		return apiProvider.createVersioningContext(apiDescriptor, beanKey.getBeanType());
	}

	public Class<?> getBeanType() {
		return beanKey.getBeanType();
	}
	
	public boolean usesServiceRegistry() {
		return this.usesServiceRegistry;
	}

}
