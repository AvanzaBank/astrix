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
package com.avanza.astrix.gs.remoting;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.openspaces.remoting.Routing;

import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.remoting.client.AmbiguousRoutingException;
import com.avanza.astrix.remoting.client.AnnotatedArgumentInstanceRouter;
import com.avanza.astrix.remoting.client.AnnotatedArgumentRouter;
import com.avanza.astrix.remoting.client.BroadcastRouter;
import com.avanza.astrix.remoting.client.DefaultAstrixRoutingStrategy;
import com.avanza.astrix.remoting.client.PropertyOnAnnotatedArgumentRoutingStrategy;
import com.avanza.astrix.remoting.client.Router;
import com.avanza.astrix.remoting.client.RoutingStrategy;
import com.avanza.astrix.remoting.util.RoutingKeyMethodCache;
import com.gigaspaces.annotation.pojo.SpaceRouting;
/**
 * Provides a routing strategy compatible with the routing used by native 
 * gigaspaces remoting, (@Routing annotated arguments).
 * 
 * @author Elias Lindholm
 *
 */
public class GsRoutingStrategy implements RoutingStrategy {
	
	private static final RoutingKeyMethodCache<SpaceRouting> routingKeyMethodCache = new RoutingKeyMethodCache<>(SpaceRouting.class);

	@Override
	public Router create(Method serviceMethod) {
		return createRoutingStrategy(serviceMethod);
	}
	
	private Router createRoutingStrategy(Method m) {
		Router result = new DefaultAstrixRoutingStrategy().create(m);
		if (result != null) {
			return result;
		}
		result = lookForRoutingAnnotationInMethodSignature(m);
		if (result != null) {
			return result;
		}
		result = lookForRoutingAnnotationOnMethodArguments(m);
		if (result != null) {
			return result;
		}
		if (m.getAnnotation(AstrixBroadcast.class) == null) {
			throw new AmbiguousRoutingException(String.format("Ambiguous routing. No routing argument defined in method signature or on method " +
					"arguments and method not annotated with @AstrixBroadcast. serviceMethod=%s", m.toString()));
		}
		return new BroadcastRouter();
	}

	private Router lookForRoutingAnnotationOnMethodArguments(Method m) {
		Router result = null;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			Method routingKeyMethod = routingKeyMethodCache.getRoutingKeyMethod(m.getParameterTypes()[argumentIndex]);
			if (routingKeyMethod != null) {
				if (result != null) {
					throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple arguments with @SpaceRouting annotated methods." +
							" Use @Routing on one service argument to identify routing method, or @AstrixBroadcast for broadcast" +
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
				if (a.annotationType().equals(Routing.class)) {
					if (routingStrategy != null) {
						throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple @Routing annotated methods on %s", m.toString()));
					}
					routingStrategy = createRoutingStrategy(m, argumentIndex, (Routing) a);
				}
			}
		}
		return routingStrategy;
	}

	public static Router createRoutingStrategy(Method serviceMethod, int routingArgumentIndex, Routing routingAnnotation) {
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
