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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InstanceCache {
	
	private final Logger logger = LoggerFactory.getLogger(InstanceCache.class);
	private final ConcurrentMap<Class<?>, Object> instanceByType = new ConcurrentHashMap<>();
	private final ObjectInitializer initializer;
	
	public InstanceCache(ObjectInitializer initializer) {
		this.initializer = Objects.requireNonNull(initializer);
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> type) {
		T object = (T) this.instanceByType.get(type);
		if (object != null) {
			return object;
		}
		return init(type);
	}
	
	public void destroy() {
		// TODO: ensure that no beans can be created before destroying instances.
		for (Object object : this.instanceByType.values()) {
			List<Method> methods = getMethodsAnnotatedWith(object.getClass(), PreDestroy.class);
			for (Method m : methods) {
				try {
					m.invoke(object);
				} catch (Exception e) {
					logger.error(String.format("Failed to invoke destroy method. methodName=%s objectType=%s", m.getName(), object.getClass().getName()), e);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private synchronized <T> T init(Class<T> type) {
		T object = (T) this.instanceByType.get(type);
		if (object != null) {
			return object;
		}
		T instance;
		try {
			instance = type.newInstance();
			initializer.init(instance);
			this.instanceByType.put(type, instance);
			return instance;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to create instance of: " + type);
		}
	}
	
	interface ObjectInitializer {
		void init(Object object);
	}
	
	private static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
	    final List<Method> methods = new ArrayList<Method>();
	    for (Method method : type.getMethods()) { 
            if (method.isAnnotationPresent(annotation)) {
                methods.add(method);
            }
	    }
	    return methods;
	}
	
}
