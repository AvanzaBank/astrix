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
package com.avanza.astrix.modules;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

final class ClassConstructorFactory<T> {
	
	private Class<T> factory;
	
	public ClassConstructorFactory(Class<T> factory) {
		this.factory = factory;
	}
	
	public T create(Dependencies dependencies) {
		Constructor<T> factory = getFactory();
		Object[] args = new Object[factory.getParameterTypes().length];
		for (int argumentIndex = 0; argumentIndex < factory.getParameterTypes().length; argumentIndex++) {
			Class<?> argumentType = factory.getParameterTypes()[argumentIndex];
			if (argumentType.isAssignableFrom(List.class)) {
				ParameterizedType listType = (ParameterizedType) factory.getGenericParameterTypes()[argumentIndex];
				args[argumentIndex] = new ArrayList<>(dependencies.getAll(getElementType(listType)));
			} else {
				args[argumentIndex] = dependencies.get(argumentType);
			}
		}
		factory.setAccessible(true);
		T instance = newInstance(factory, args);
		runMethodInjection(instance, dependencies);
		return instance;
	}
	
	private static <T> T newInstance(Constructor<T> factory, Object[] args) {
		try {
			return factory.newInstance(args);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Failed to instantiate class using constructor: " + factory, e);
		}
	}

	private void runMethodInjection(Object target, Dependencies beans) {
		for (Method m : target.getClass().getMethods()) {
			if (!m.isAnnotationPresent(AstrixInject.class)) {
				continue;
			}
			if (m.getParameterTypes().length == 0) {
				throw new IllegalArgumentException(String.format("@AstrixInject annotated methods must accept at least one dependency. Class: %s, method: %s"
						, target.getClass().getName()
						, m.getName()));
			}
			Object[] deps = new Object[m.getParameterTypes().length];
			for (int argIndex = 0; argIndex < deps.length; argIndex++) {
				Class<?> dep = m.getParameterTypes()[argIndex];
				deps[argIndex] = beans.get(dep);
			}
			try {
				m.invoke(target, deps);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Failed to inject dependencies into class instantiated by modules: " + target.getClass().getName(), e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E> Class<E> getElementType(ParameterizedType listType) {
		Type actualTypeArguments = listType.getActualTypeArguments()[0];
		if (actualTypeArguments instanceof Class) {
			return (Class) actualTypeArguments;
		} else {
			return (Class) ParameterizedType.class.cast(actualTypeArguments).getRawType();
		}
	}

	@SuppressWarnings("unchecked")
	private Constructor<T> getFactory() {
		Constructor<?>[] constructors = factory.getDeclaredConstructors();
		if (constructors.length == 0) {
			throw new IllegalStateException("Couldn't find public constructor on: " + factory.getName());
		}
		if (constructors.length == 1) {
			return (Constructor<T>) constructors[0];
		}
		for (Constructor<?> constructor : constructors) {
			if (isAnnotationPresent(constructor, AstrixInject.class)) {
				return (Constructor<T>) constructor;
			}
		}
		throw new IllegalStateException("Multiple constructors found on class and no @AstrixInject annotated constructor was found on: "  + factory.getName());
	}

	private boolean isAnnotationPresent(Constructor<?> constructor, Class<? extends Annotation> annotation) {
		for (Annotation a : constructor.getAnnotations()) {
			if (annotation.isAssignableFrom(a.getClass())) {
				return true;
			}
		}
		return false;
	}

}