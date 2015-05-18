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
package com.avanza.astrix.beans.service;

import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;



/**
 * Holds static information about a given service, i.e information defined
 * in the ApiProvider as part of the service definition. 
 * 
 * This is different from the dynamic information stored in 
 * the service registry which is represented by {@link ServiceProperties}.
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class ServiceContext {
	
	private Class<?> serviceConfigClass = null;

	private ServiceContext(Class<?> serviceConfigClass) {
		this.serviceConfigClass = serviceConfigClass;
	}
	
	public abstract boolean isVersioned();
	
	@SuppressWarnings("unchecked")
	public <T> Class<T> getServiceConfigClass(Class<T> type) {
		if (this.serviceConfigClass == null) {
			throw new IllegalStateException(String.format("Expected service configuration class of type=%s to be defined for service", type.getName()));
		}
		if (!type.isAssignableFrom(this.serviceConfigClass)) {
			throw new IllegalStateException(String.format("Expected service configuration class of type=%s, but actualType=", type.getName(), serviceConfigClass.getName()));
		} 
		return (Class<T>) serviceConfigClass;
	}
	
	public abstract int version();
	
	public abstract Class<? extends AstrixObjectSerializerConfigurer> getObjectSerializerConfigurerClass();
	
	public static ServiceContext versionedService(int version, Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer) {
		return versionedService(version, objectSerializerConfiguer, null); 
	}
	
	public static ServiceContext versionedService(
			int version,
			Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfigurer,
			Class<?> serviceConfigClass) {
		
		return new VersionedServiceContext(version, objectSerializerConfigurer, serviceConfigClass);
	}
	
	public static ServiceContext nonVersioned() {
		return nonVersioned(null);
	}
	
	public static ServiceContext nonVersioned(Class<?> serviceConfig) {
		return new NonVersionedServiceContext(serviceConfig);
	}
	
	private static class VersionedServiceContext extends ServiceContext {
		
		private int version;
		private Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer;
		
		public VersionedServiceContext(int version,
									   Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer,
									   Class<?> serviceConfigClass) {
			super(serviceConfigClass);
			this.version = version;
			this.objectSerializerConfiguer = objectSerializerConfiguer;
		}

		@Override
		public boolean isVersioned() {
			return true;
		}
		
		@Override
		public int version() {
			return this.version;
		}

		@Override
		public Class<? extends AstrixObjectSerializerConfigurer> getObjectSerializerConfigurerClass() {
			return this.objectSerializerConfiguer;
		}
	}
	
	private static class NonVersionedServiceContext extends ServiceContext {

		public NonVersionedServiceContext(Class<?> serviceConfig) {
			super(serviceConfig);
		}

		@Override
		public boolean isVersioned() {
			return false;
		}
		
		@Override
		public int version() {
			throw new IllegalStateException("Non-versioned services does not provide a version");
		}

		@Override
		public Class<? extends AstrixObjectSerializerConfigurer> getObjectSerializerConfigurerClass() {
			throw new IllegalStateException("Non-versioned services does not provide a AstrixObjectSerializerConfigurer");
		}
	}

}
