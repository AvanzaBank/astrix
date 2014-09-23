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

import se.avanzabank.asterix.provider.core.AsterixService;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceDescriptor {

	private final Class<?> descriptorHolder;
	private final AsterixApiDescriptor apiDescriptor;
	private AsterixService asterixService;

	public AsterixServiceDescriptor(Class<?> descriptorHolder) {
		this.asterixService = descriptorHolder.getAnnotation(AsterixService.class);
		this.descriptorHolder = descriptorHolder;
		this.apiDescriptor = new AsterixApiDescriptor(asterixService.apiDescriptors()[0]);
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return apiDescriptor.isAnnotationPresent(annotationClass);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return apiDescriptor.getAnnotation(annotationClass);
	}
	
	public String getName() {
		return this.descriptorHolder.getName();
	}

	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
	}

	public Class<?> getHolder() {
		return descriptorHolder;
	}

	public AsterixApiDescriptor getApiDescriptor() {
		return this.apiDescriptor;
	}
	
}
