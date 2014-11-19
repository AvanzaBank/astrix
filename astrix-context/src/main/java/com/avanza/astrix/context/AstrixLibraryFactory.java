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
package com.avanza.astrix.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.avanza.astrix.provider.core.AstrixQualifier;

public class AstrixLibraryFactory<T> implements AstrixFactoryBeanPlugin<T>, AstrixBeanAware {

	private Object factoryInstance;
	private Method factoryMethod;
	private Class<T> type;
	private List<AstrixBeanKey> astrixBeanDependencies;
	private AstrixBeans beans;
	
	@SuppressWarnings("unchecked")
	public AstrixLibraryFactory(Object factoryInstance, Method factoryMethod) {
		this.factoryInstance = factoryInstance;
		this.factoryMethod = factoryMethod;
		this.type = (Class<T>) factoryMethod.getReturnType();
		this.astrixBeanDependencies = new ArrayList<>(factoryMethod.getParameterTypes().length);
		for (int argumentIndex = 0; argumentIndex < factoryMethod.getParameterTypes().length; argumentIndex++) {
			Class<?> parameterType = factoryMethod.getParameterTypes()[argumentIndex];
			String parameterQualifier = getParameterQualifier(argumentIndex);
			astrixBeanDependencies.add(AstrixBeanKey.create(parameterType, parameterQualifier));
		}
	}

	@Override
	public T create(String qualifier) {
		Object[] args = new Object[factoryMethod.getParameterTypes().length];
		for (int argumentIndex = 0; argumentIndex < factoryMethod.getParameterTypes().length; argumentIndex++) {
			AstrixBeanKey dep = astrixBeanDependencies.get(argumentIndex);
			Class<?> argumentType = dep.getBeanType();
			String parameterQualifier = dep.getQualifier();
			args[argumentIndex] = beans.getBean(argumentType, parameterQualifier);
		}
		Object result;
		try {
			result = factoryMethod.invoke(factoryInstance, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate library using: " + factoryMethod.toString() + " on " + type.getName(), e);
		}
		return type.cast(result);
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
	public Class<T> getBeanType() {
		return this.type;
	}
	
	@Override
	public List<AstrixBeanKey> getBeanDependencies() {
		return this.astrixBeanDependencies;
	}

	@Override
	public void setAstrixBeans(AstrixBeans beans) {
		this.beans = beans;
	}
	
}
