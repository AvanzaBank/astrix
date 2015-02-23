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
package com.avanza.astrix.remoting.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.avanza.astrix.core.AstrixRouting;
import com.avanza.astrix.remoting.util.RoutingKeyMethodCache;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class DefaultAstrixRoutingStrategy implements RoutingStrategy {

	private RoutingKeyMethodCache<AstrixRouting> routingKeyMethodCache = new RoutingKeyMethodCache<>(AstrixRouting.class);

	@Override
	public Router create(Method serviceMethod) {
		return createRoutingStrategy(serviceMethod);
	}
	
	private Router createRoutingStrategy(Method m) {
		Router result = lookForRoutingAnnotationInMethodSignature(m);
		if (result != null) {
			return result;
		}
		result = lookForRoutingAnnotationOnMethodArguments(m);
		if (result != null) {
			return result;
		}
		return null;
	}

	private Router lookForRoutingAnnotationOnMethodArguments(Method m) {
		Router result = null;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			Method routingKeyMethod = routingKeyMethodCache.getRoutingKeyMethod(m.getParameterTypes()[argumentIndex]);
			if (routingKeyMethod != null) {
				if (result != null) {
					throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple arguments with @AstrixRouting annotated methods." +
							" Use @AstrixRouting on one service argument to identify routing method, or @AstrixBroadcast for broadcast" +
							" operations. service method=%s", m.toString()));
				}
				result = new AnnotatedArgumentInstanceRouter(argumentIndex, routingKeyMethod);
			}
		}
		return result;
	}
	
	public static Router lookForRoutingAnnotationInMethodSignature(Method m) {
		Router routingStrategy = null;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			for (Annotation a : m.getParameterAnnotations()[argumentIndex]) {
				if (a.annotationType().equals(AstrixRouting.class)) {
					if (routingStrategy != null) {
						throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple @AstrixRouting annotated methods on %s", m.toString()));
					}
					routingStrategy = createRoutingStrategy(m, argumentIndex, (AstrixRouting) a);
				}
			}
		}
		return routingStrategy;
	}

	public static Router createRoutingStrategy(Method serviceMethod, int routingArgumentIndex, AstrixRouting routingAnnotation) {
		if (routingAnnotation.value().trim().isEmpty()) {
			return new AnnotatedArgumentRouter(routingArgumentIndex);
		} 
		String targetRoutingMethod = routingAnnotation.value();
		try {
			Class<?> type = serviceMethod.getParameterTypes()[routingArgumentIndex];
			Method method = type.getMethod(targetRoutingMethod);
			return new PropertyOnAnnotatedArgumentRoutingStrategy(routingArgumentIndex, method);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Cant route using: " + targetRoutingMethod , e);
		}
	}

}
