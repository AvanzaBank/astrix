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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;





/**
 * Holds static information about a given service, i.e information defined
 * in the ApiProvider as part of the service definition. 
 * 
 * This is different from the dynamic information stored in 
 * the service registry which is represented by {@link ServiceProviderInstanceProperties}.
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public final class ServiceDefinition<T> {
	
	private final Class<?> serviceConfigClass;
	private final ObjectSerializerDefinition objectSerializerDefinition;
	private final AstrixBeanKey<T> beanKey;
	private final boolean dynamicQualified;
	private final ServiceDefinitionSource serviceDefinitionSource;
	
	public ServiceDefinition(ServiceDefinitionSource serviceDefinitionSource, AstrixBeanKey<T> beanKey, ObjectSerializerDefinition serializerDefinition, boolean dynamicQualified) {
		this.serviceDefinitionSource = serviceDefinitionSource;
		this.beanKey = beanKey;
		this.objectSerializerDefinition = serializerDefinition;
		this.dynamicQualified = dynamicQualified;
		this.serviceConfigClass = null;
	}

	private ServiceDefinition(ServiceDefinitionSource serviceDefinitionSource, AstrixBeanKey<T> beanKey, Class<?> serviceConfigClass, ObjectSerializerDefinition objectSerializerDefinition, boolean dynamicQualified) {
		this.serviceDefinitionSource = serviceDefinitionSource;
		this.beanKey = beanKey;
		this.serviceConfigClass = serviceConfigClass;
		this.objectSerializerDefinition = objectSerializerDefinition;
		this.dynamicQualified = dynamicQualified;
	}

	public static <T> ServiceDefinition<T> create(ServiceDefinitionSource serviceDefinitionSource,
												  AstrixBeanKey<T> beanKey,
												  Class<?> serviceConfigClass,
												  ObjectSerializerDefinition serializerDefinition,
												  boolean dynamicQualified) {
		return new ServiceDefinition<>(serviceDefinitionSource, beanKey, serviceConfigClass, serializerDefinition, dynamicQualified);
	}
	
	public <E> ServiceDefinition<E> asyncDefinition(Class<E> asyncInterface) {
		return new ServiceDefinition<>(serviceDefinitionSource, AstrixBeanKey.create(asyncInterface, beanKey.getQualifier()), serviceConfigClass, objectSerializerDefinition, this.dynamicQualified);
	}
	
	public boolean isDynamicQualified() {
		return dynamicQualified;
	}
	
	public Class<T> getServiceType() {
		return this.beanKey.getBeanType();
	}
	
	public ServiceDefinitionSource getServiceDefinitionSource() {
		return serviceDefinitionSource;
	}
	
	public boolean isVersioned() {
		return objectSerializerDefinition.isVersioned();
	}
	
	@SuppressWarnings("unchecked")
	public <E> Class<E> getServiceConfigClass(Class<E> type) {
		if (this.serviceConfigClass == null) {
			throw new IllegalStateException(String.format("Expected service configuration class of type=%s to be defined for service", type.getName()));
		}
		if (!type.isAssignableFrom(this.serviceConfigClass)) {
			throw new IllegalStateException(String.format("Expected service configuration class of type=%s, but actualType=%s", type.getName(), serviceConfigClass.getName()));
		} 
		return (Class<E>) serviceConfigClass;
	}
	
	public int version() {
		return objectSerializerDefinition.version();
	}
	
	public ObjectSerializerDefinition getObjectSerializerDefinition() {
		return this.objectSerializerDefinition;
	}

	public AstrixBeanKey<T> getBeanKey() {
		return this.beanKey;
	}
}
