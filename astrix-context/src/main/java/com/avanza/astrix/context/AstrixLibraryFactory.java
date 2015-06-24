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
package com.avanza.astrix.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.provider.core.AstrixQualifier;

public class AstrixLibraryFactory<T> implements StandardFactoryBean<T> {

	private Object factoryInstance;
	private Method factoryMethod;
	private AstrixBeanKey<T> beanKey;
	
	@SuppressWarnings("unchecked")
	public AstrixLibraryFactory(Object factoryInstance, Method factoryMethod, String qualifier) {
		this.factoryInstance = factoryInstance;
		this.factoryMethod = factoryMethod;
		this.beanKey = AstrixBeanKey.create((Class<T>) factoryMethod.getReturnType(), qualifier);

	}

	@Override
	public T create(AstrixBeans beans) {
		List<AstrixBeanKey<?>> beanDependencies = new ArrayList<>(factoryMethod.getParameterTypes().length);
		for (int argumentIndex = 0; argumentIndex < factoryMethod.getParameterTypes().length; argumentIndex++) {
			Class<?> parameterType = factoryMethod.getParameterTypes()[argumentIndex];
			String parameterQualifier = getParameterQualifier(argumentIndex);
			beanDependencies.add(AstrixBeanKey.create(parameterType, parameterQualifier));
		}
		
		Object[] args = new Object[factoryMethod.getParameterTypes().length];
		for (int argumentIndex = 0; argumentIndex < factoryMethod.getParameterTypes().length; argumentIndex++) {
			AstrixBeanKey<?> dep = beanDependencies.get(argumentIndex);
			Class<?> argumentType = dep.getBeanType();
			String parameterQualifier = dep.getQualifier();
			args[argumentIndex] = beans.getBean(AstrixBeanKey.create(argumentType, parameterQualifier));
		}
		Object result;
		try {
			result = factoryMethod.invoke(factoryInstance, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate library using: " + factoryMethod.toString() + " on " + factoryInstance.getClass().getName(), e);
		}
		return beanKey.getBeanType().cast(result);
	}

	private String getParameterQualifier(int argumentIndex) {
		for (Annotation parameterAnnotation : factoryMethod.getParameterAnnotations()[argumentIndex]) {
			if (parameterAnnotation instanceof AstrixQualifier) {
				return AstrixQualifier.class.cast(parameterAnnotation).value();
			}
		}
		return null;
	}
	
	@Override
	public AstrixBeanKey<T> getBeanKey() {
		return this.beanKey;
	}
	
	@Override
	public boolean lifecycled() {
		return true;
	}
	
}
