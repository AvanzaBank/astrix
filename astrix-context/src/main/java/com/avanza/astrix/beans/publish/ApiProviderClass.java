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
package com.avanza.astrix.beans.publish;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class ApiProviderClass {
	
	public static ApiProviderClass create(Class<?> providerClass) {
		return new ApiProviderClass(providerClass);
	}
	private Class<?> providerClass;

	private ApiProviderClass(Class<?> annotationHolder) {
		this.providerClass = annotationHolder;
	}
	
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return providerClass.isAnnotationPresent(annotationClass);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return providerClass.getAnnotation(annotationClass);
	}
	
	public String getProviderClassName() {
		return this.providerClass.getName();
	}
	
	public String getName() {
		return this.providerClass.getSimpleName();
	}

	public Class<?> getProviderClass() {
		return providerClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(providerClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApiProviderClass other = (ApiProviderClass) obj;
		return Objects.equals(providerClass, other.providerClass);
	}
	
	@Override
	public final String toString() {
		return getProviderClassName();
	}

	public List<BeanDefinitionMethod<?>> getBeanDefinitionMethods() {
		List<BeanDefinitionMethod<?>> result = new LinkedList<>();
		for (Method astrixBeanDefinitionMethod : getProviderClass().getMethods()) {
			result.add(BeanDefinitionMethod.create(astrixBeanDefinitionMethod));
		}
		return result;
	}
	
}
