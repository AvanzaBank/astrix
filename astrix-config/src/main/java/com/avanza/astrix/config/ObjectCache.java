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
package com.avanza.astrix.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Manages the life-cycle of each object created by Astrix. Each object created will be cached.
 * 
 * When the object is created (upon cache-miss) the object will be created using the ObjectFactory
 * and then the object will be initialized by invoking all @PostConstruct annotated methods.
 * 
 * When the ObjectCache is destroyed all objects in the cache will be destroyed by invoking
 * all @PreDestroy annotated methods on every instance in the cache.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class ObjectCache {
	
	private static final Logger logger = LoggerFactory.getLogger(ObjectCache.class);
	private final ConcurrentMap<Object, Object> instanceById = new ConcurrentHashMap<>();
	private final ConcurrentMap<Object, Lock> lockedObjects = new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Object objectId, ObjectFactory<T> factory) {
		T object = (T) this.instanceById.get(objectId);
		if (object != null) {
			return object;
		}
		return create(objectId, factory);
	}
	
	public void destroy() {
		for (Object object : this.instanceById.values()) {
			destroy(object);
		}
	}

	public static void destroy(Object object) {
		List<Method> methods = getMethodsAnnotatedWith(object.getClass(), PreDestroy.class);
		for (Method m : methods) {
			try {
				m.invoke(object);
			} catch (Exception e) {
				logger.error(String.format("Failed to invoke destroy method. methodName=%s objectType=%s", m.getName(), object.getClass().getName()), e);
			}
		}
	}
	
	public static void init(Object object) {
		List<Method> methods = getMethodsAnnotatedWith(object.getClass(), PostConstruct.class);
		for (Method m : methods) {
			try {
				m.invoke(object);
			} catch (Exception e) {
				logger.error(String.format("Failed to invoke init method. methodName=%s objectType=%s", m.getName(), object.getClass().getName()), e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T create(Object id, ObjectFactory<T> objectFactory) {
		Lock lock = new ReentrantLock();
		Lock existingLock = lockedObjects.putIfAbsent(id, lock);
		if (existingLock != null) {
			// Another thread is already crating  object
			lock = existingLock;
		}
		lock.lock();
		try {
			T object = (T) this.instanceById.get(id);
			if (object != null) {
				// Another thread created instance
				return object;
			}
			// Create instance
			T instance;
			try {
				instance = objectFactory.create();
				init(instance);
				this.instanceById.put(id, instance);
				return instance;
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Failed to create instance of: " + id);
			}
		} finally {
			lock.unlock();
			/*
			 * If object was created on current thread, then remove the lock.
			 * If another thread created the object, this will have no effect.
			 */
			lockedObjects.remove(id);
		}
	}
	
	public <T> T create(Object id, final T instance) {
		return create(id, new ObjectFactory<T>() {
			@Override
			public T create() throws Exception {
				return instance;
			}
		});
	}
	
	public interface ObjectFactory<T> {
		T create() throws Exception;
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
