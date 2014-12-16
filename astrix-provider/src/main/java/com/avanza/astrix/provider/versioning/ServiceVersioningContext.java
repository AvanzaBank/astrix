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
package com.avanza.astrix.provider.versioning;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class ServiceVersioningContext {
	
	private ServiceVersioningContext() {
	}
	
	public abstract boolean isVersioned();
	
	public abstract int version();
	
	public abstract Class<? extends AstrixObjectSerializerConfigurer> getObjectSerializerConfigurerClass();
	
	public static ServiceVersioningContext versionedService(AstrixVersioned versioned) {
		return new VersionedServiceContext(versioned.version(), versioned.objectSerializerConfigurer());
	}
	
	public static ServiceVersioningContext versionedService(int version, Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer) {
		return new VersionedServiceContext(version, objectSerializerConfiguer);
	}
	
	public static ServiceVersioningContext nonVersioned() {
		return new NonVersionedServiceContext();
	}
	
	private static class VersionedServiceContext extends ServiceVersioningContext {
		
		private int version;
		private Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer;
		
		public VersionedServiceContext(int version,
									   Class<? extends AstrixObjectSerializerConfigurer> objectSerializerConfiguer) {
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
	
	private static class NonVersionedServiceContext extends ServiceVersioningContext {

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
