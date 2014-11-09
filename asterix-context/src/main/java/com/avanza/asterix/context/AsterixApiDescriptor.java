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
package com.avanza.asterix.context;

import java.lang.annotation.Annotation;

import com.avanza.asterix.provider.core.AsterixServiceRegistryApi;
import com.avanza.asterix.provider.core.AsterixSubsystem;
import com.avanza.asterix.provider.versioning.AsterixVersioned;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class AsterixApiDescriptor {

	public static AsterixApiDescriptor create(Class<?> descriptorHolder) {
		return new AnnotationApiDescriptor(descriptorHolder);
	}
	
	public static AsterixApiDescriptor simple(String name, String subsystem) {
		return new SimpleApiDescriptor(name, subsystem);
	}

	public abstract boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

	public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);
	
	public abstract String getName();
	
	public abstract Class<?> getDescriptorClass();
	
	@Override
	public final String toString() {
		return getName();
	}
	
	/**
	 * Whether this api uses the service registry or not.
	 * @return
	 */
	public abstract boolean usesServiceRegistry();
	
	public abstract boolean isVersioned();
	
	public abstract String getSubsystem();
	
	private static class SimpleApiDescriptor extends AsterixApiDescriptor {

		private String name;
		private String subsystem;
		private AsterixFactoryBeanPlugin<?> factory;
		
		public SimpleApiDescriptor(String name, String subsystem) {
			this.name = name;
			this.subsystem = subsystem;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			return false;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Class<?> getDescriptorClass() {
			return null;
		}

		@Override
		public boolean usesServiceRegistry() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		@Override
		public String getSubsystem() {
			return subsystem;
		}
		
		public AsterixFactoryBeanPlugin<?> getFactory() {
			return factory;
		}
		
	}
	
	private static class AnnotationApiDescriptor extends AsterixApiDescriptor {
		private Class<?> descriptorHolder;

		private AnnotationApiDescriptor(Class<?> annotationHolder) {
			this.descriptorHolder = annotationHolder;
		}
		
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			return descriptorHolder.isAnnotationPresent(annotationClass);
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return descriptorHolder.getAnnotation(annotationClass);
		}
		
		public String getName() {
			return this.descriptorHolder.getName();
		}

		public Class<?> getDescriptorClass() {
			return descriptorHolder;
		}
		
		/**
		 * Whether this api uses the service registry or not.
		 * @return
		 */
		public boolean usesServiceRegistry() {
			return descriptorHolder.isAnnotationPresent(AsterixServiceRegistryApi.class);
		}
		
		public boolean isVersioned() {
			return descriptorHolder.isAnnotationPresent(AsterixVersioned.class);
		}
		
		public String getSubsystem() {
			if (!descriptorHolder.isAnnotationPresent(AsterixSubsystem.class)) {
				return null;
			}
			AsterixSubsystem subsystem = descriptorHolder.getAnnotation(AsterixSubsystem.class);
			return subsystem.value();
		}
	}

}
