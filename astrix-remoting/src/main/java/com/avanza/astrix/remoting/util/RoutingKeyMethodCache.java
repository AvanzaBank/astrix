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
package com.avanza.astrix.remoting.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class RoutingKeyMethodCache<T extends Annotation> {
	
	private final ConcurrentMap<Class<?>, CachedRoutingKeyMethod> routingKeyMethodByType = new ConcurrentHashMap<>();
	private final RoutingKeyMethodScanner scanner = new RoutingKeyMethodScanner();
	private final Class<T> routingAnnotation;
	
	public RoutingKeyMethodCache(Class<T> routingAnnotation) {
		this.routingAnnotation = routingAnnotation;
	}

	public Method getRoutingKeyMethod(Class<?> spaceObjectClass) {
		CachedRoutingKeyMethod cachedMethod = routingKeyMethodByType.get(spaceObjectClass);
		if (cachedMethod != null) {
			return cachedMethod.get(); 
		}
		Method routingKeyMethod = scanner.getRoutingKeyMethod(routingAnnotation, spaceObjectClass);
		routingKeyMethodByType.putIfAbsent(spaceObjectClass, new CachedRoutingKeyMethod(routingKeyMethod));
		return routingKeyMethod;
	}
	
	private static class CachedRoutingKeyMethod {
		private Method method;
		public CachedRoutingKeyMethod(Method method) {
			this.method = method;
		}
		public Method get() {
			return method;
		}
	}
	

}
