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
package com.avanza.astrix.beans.service;

import java.lang.annotation.Annotation;

import com.avanza.astrix.beans.core.AstrixBeanKey;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public final class ServiceDiscoveryFactory<T extends Annotation> {
	
	private final ServiceDiscoveryMetaFactoryPlugin<T> factory;
	private final T annotation;
	private final Class<?> beanType;
	
	public ServiceDiscoveryFactory(ServiceDiscoveryMetaFactoryPlugin<T> factory, T annotation, Class<?> serviceBeanType) {
		this.factory = factory;
		this.annotation = annotation;
		this.beanType = serviceBeanType;
	}

	public ServiceDiscovery create(String beanQualifier) {
		return factory.create(AstrixBeanKey.create(this.beanType, beanQualifier), annotation);
	}
	

}
