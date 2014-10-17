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
package se.avanzabank.asterix.context;

import java.lang.annotation.Annotation;

import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.provider.core.AsterixSubsystem;
import se.avanzabank.asterix.provider.library.AsterixLibraryProvider;
import se.avanzabank.asterix.provider.versioning.AsterixVersioned;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixApiDescriptor {
	
	private final Class<?> descriptorHolder;

	public AsterixApiDescriptor(Class<?> descriptorHolder) {
		this.descriptorHolder = descriptorHolder;
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
	
	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
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
	
	public boolean isLibrary() {
		return descriptorHolder.isAnnotationPresent(AsterixLibraryProvider.class);
	}

	public String getSubsystem() {
		if (!descriptorHolder.isAnnotationPresent(AsterixSubsystem.class)) {
			return null;
		}
		AsterixSubsystem subsystem = descriptorHolder.getAnnotation(AsterixSubsystem.class);
		return subsystem.value();
	}
}
