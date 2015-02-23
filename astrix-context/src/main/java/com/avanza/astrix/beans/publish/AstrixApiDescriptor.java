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
package com.avanza.astrix.beans.publish;

import java.lang.annotation.Annotation;
import java.util.Objects;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixApiDescriptor {

	public static AstrixApiDescriptor create(Class<?> descriptorHolder) {
		return new AstrixApiDescriptor(descriptorHolder);
	}
	private Class<?> descriptorHolder;

	private AstrixApiDescriptor(Class<?> annotationHolder) {
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

	@Override
	public int hashCode() {
		return Objects.hash(descriptorHolder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstrixApiDescriptor other = (AstrixApiDescriptor) obj;
		return Objects.equals(descriptorHolder, other.descriptorHolder);
	}
	
	@Override
	public final String toString() {
		return getName();
	}
	
}
