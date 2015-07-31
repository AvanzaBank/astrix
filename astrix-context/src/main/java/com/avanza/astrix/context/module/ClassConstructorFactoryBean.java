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
package com.avanza.astrix.context.module;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.provider.core.AstrixQualifier;

final class ClassConstructorFactoryBean<T> implements StandardFactoryBean<T> {
	
	private AstrixBeanKey<T> beanKey;
	private Class<? extends T> beanImplClass;
	
	public ClassConstructorFactoryBean(AstrixBeanKey<T> beanKey, Class<? extends T> factory) {
		this.beanKey = beanKey;
		this.beanImplClass = factory;
	}

	@Override
	public T create(AstrixBeans beans) {
		try {
			return doCreate(beans);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	private T doCreate(AstrixBeans beans) throws NoSuchMethodException, SecurityException {
		Constructor<T> factory = getFactory();
		List<AstrixBeanKey<?>> beanDependencies = new ArrayList<>(factory.getParameterTypes().length);
		for (int argumentIndex = 0; argumentIndex < factory.getParameterTypes().length; argumentIndex++) {
			Class<?> parameterType = factory.getParameterTypes()[argumentIndex];
			String parameterQualifier = getParameterQualifier(factory, argumentIndex);
			beanDependencies.add(AstrixBeanKey.create(parameterType, parameterQualifier));
		}
		
		Object[] args = new Object[factory.getParameterTypes().length];
		for (int argumentIndex = 0; argumentIndex < factory.getParameterTypes().length; argumentIndex++) {
			AstrixBeanKey<?> dep = beanDependencies.get(argumentIndex);
			Class<?> argumentType = dep.getBeanType();
			String parameterQualifier = dep.getQualifier();
			if (argumentType.isAssignableFrom(List.class)) {
				ParameterizedType genericType = (ParameterizedType) factory.getGenericParameterTypes()[argumentIndex];
				Type actualTypeArguments = genericType.getActualTypeArguments()[0];
				if (actualTypeArguments instanceof Class) {
					args[argumentIndex] = new ArrayList<>(getBeansOfType(beans, (Class) actualTypeArguments));
				} else {
					args[argumentIndex] = new ArrayList<>(getBeansOfType(beans, (Class) ParameterizedType.class.cast(actualTypeArguments).getRawType()));
				}
			} else {
				args[argumentIndex] = beans.getBean(AstrixBeanKey.create(argumentType, parameterQualifier));
			}
		}
		try {
			factory.setAccessible(true);
			return factory.newInstance(args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate bean using constructor: " + factory.getName(), e);
		}
	}

	private <E> Set<E> getBeansOfType(AstrixBeans beans, Class<E> argumentType) {
		Set<AstrixBeanKey<E>> beansOfType = beans.getBeansOfType(argumentType);
		Set<E> allBeansOfType = new HashSet<>(beansOfType.size());
		for (AstrixBeanKey<E> beanKey : beansOfType) {
			allBeansOfType.add(beans.getBean(beanKey));
		}
		return allBeansOfType;
	}

	private Constructor<T> getFactory() throws NoSuchMethodException {
		Constructor<?>[] constructors = beanImplClass.getDeclaredConstructors();
		if (constructors.length == 0) {
			throw new IllegalStateException("Couldnt find public constructor on: " + beanImplClass.getName());
		}
		if (constructors.length == 1) {
			return (Constructor<T>) constructors[0];
		}
		for (Constructor<?> constructor : constructors) {
			if (isAnnotationPresent(constructor, AstrixInject.class)) {
				return (Constructor<T>) constructor;
			}
		}
		throw new IllegalStateException("Multiple constructors found on class and no @AstrixInject annotated constructor was found on: "  + beanImplClass.getName());
	}

	private boolean isAnnotationPresent(Constructor<?> constructor, Class<? extends Annotation> annotation) {
		for (Annotation a : constructor.getAnnotations()) {
			if (annotation.isAssignableFrom(a.getClass())) {
				return true;
			}
		}
		return false;
	}

	private String getParameterQualifier(Constructor<T> factory, int argumentIndex) {
		for (Annotation parameterAnnotation : factory.getParameterAnnotations()[argumentIndex]) {
			if (parameterAnnotation instanceof AstrixQualifier) {
				return AstrixQualifier.class.cast(parameterAnnotation).value();
			}
		}
		return null;
	}

	@Override
	public AstrixBeanKey<T> getBeanKey() {
		return beanKey;
	}

	@Override
	public boolean lifecycled() {
		return true;
	}
}