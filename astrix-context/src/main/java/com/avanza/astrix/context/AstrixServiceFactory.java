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
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixServiceFactory<T> implements AstrixFactoryBeanPlugin<T> {
	
	private final Class<T> beanType;
	private final AstrixApiDescriptor descriptor;
	private final AstrixServiceComponents serviceComponents;
	private final AstrixServiceLookup serviceLookup;

	public AstrixServiceFactory(AstrixApiDescriptor descriptor, Class<T> beanType, AstrixServiceLookup serviceLookup, AstrixServiceComponents serviceComponents) {
		this.descriptor = descriptor;
		this.beanType = beanType;
		this.serviceLookup = serviceLookup;
		this.serviceComponents = serviceComponents;
	}

	@Override
	public T create(String optionalQualifier) {
		AstrixServiceProperties serviceProperties = serviceLookup.lookup(beanType, optionalQualifier);
		AstrixServiceComponent serviceComponent = serviceComponents.getComponent(serviceProperties.getComponent());
		return serviceComponent.createService(descriptor, beanType, serviceProperties);
	}

	@Override
	public Class<T> getBeanType() {
		return beanType;
	}
	
}
