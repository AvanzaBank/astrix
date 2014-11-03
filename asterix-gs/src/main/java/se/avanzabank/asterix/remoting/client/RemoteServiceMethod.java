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
package se.avanzabank.asterix.remoting.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.openspaces.remoting.Routing;

import se.avanzabank.asterix.core.AsterixBroadcast;
import se.avanzabank.asterix.remoting.util.MethodSignatureBuilder;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class RemoteServiceMethod {
	
	private static final RoutingKeyMethodCache routingKeyMethodCache = new RoutingKeyMethodCache();
	
	private String signature;
	private RoutingStrategy routingStrategy;
	
	private RemoteServiceMethod(String signature, RoutingStrategy routingStrategy) {
		this.signature = signature;
		this.routingStrategy = routingStrategy;
	}

	public static RemoteServiceMethod create(Method m) {
		String signature = MethodSignatureBuilder.build(m);
		RoutingStrategy routingStrategy = createRoutingStrategy(m);
		return new RemoteServiceMethod(signature, routingStrategy);
	}
	
	
	public String getSignature() {
		return signature;
	}
	
	public GsRoutingKey getRoutingKey(Object... args) throws Exception {
		return this.routingStrategy.getRoutingKey(args);
	}
	

	private static RoutingStrategy createRoutingStrategy(Method m) {
		RoutingStrategy result = lookForRoutingAnnotationInMethodSignature(m);
		if (result != null) {
			return result;
		}
		result = lookForRoutingAnnotationOnMethodArguments(m);
		if (result != null) {
			return result;
		}
		if (m.getAnnotation(AsterixBroadcast.class) == null) {
			throw new AmbiguousRoutingException(String.format("Ambiguous routing. No routing argument defined in method signature or on method " +
					"arguments and method not annotated with @AsterixBroadcast. serviceMethod=%s", m.toString()));
		}
		return new BroadcastRoutingStrategy();
	}

	private static RoutingStrategy lookForRoutingAnnotationOnMethodArguments(Method m) {
		RoutingStrategy result = null;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			Method routingKeyMethod = routingKeyMethodCache.getRoutingKeyMethod(m.getParameterTypes()[argumentIndex]);
			if (routingKeyMethod != null) {
				if (result != null) {
					throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple arguments with @SpaceRouting annotated methods." +
							" Use @Routing on one service argument to identify routing method, or @AsterixBroadcast for broadcast" +
							" operations. service method=%s", m.toString()));
				}
				result = new AnnotatedArgumentInstanceRoutingStrategy(argumentIndex, routingKeyMethod);
			}
		}
		return result;
	}
	
	private static RoutingStrategy lookForRoutingAnnotationInMethodSignature(Method m) {
		RoutingStrategy routingStrategy = null;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			for (Annotation a : m.getParameterAnnotations()[argumentIndex]) {
				if (a.annotationType().equals(Routing.class)) {
					if (routingStrategy != null) {
						throw new AmbiguousRoutingException(String.format("Ambiguous routing, multiple @Routing annotated methods on %s", m.toString()));
					}
					routingStrategy = new AnnotatedArgumentRoutingStrategy(argumentIndex);
				}
			}
		}
		return routingStrategy;
	}
	
	private interface RoutingStrategy {
		GsRoutingKey getRoutingKey(Object[] args) throws Exception;
	}
	
	private static class AnnotatedArgumentRoutingStrategy implements RoutingStrategy {
		
		private int argumentIndex;

		public AnnotatedArgumentRoutingStrategy(int argumentIndex) {
			this.argumentIndex = argumentIndex;
		}

		@Override
		public GsRoutingKey getRoutingKey(Object[] args) {
			return GsRoutingKey.create(args[argumentIndex]);
		}
	}
	
	private static class AnnotatedArgumentInstanceRoutingStrategy implements RoutingStrategy {
		
		private int argumentIndex;
		private Method routingKeyMethod;
		
		public AnnotatedArgumentInstanceRoutingStrategy(int argumentIndex, Method routingKeyMethod) {
			this.argumentIndex = argumentIndex;
			this.routingKeyMethod = routingKeyMethod;
		}

		@Override
		public GsRoutingKey getRoutingKey(Object[] args) throws Exception {
			return GsRoutingKey.create(routingKeyMethod.invoke(args[argumentIndex]));
		}
	}
	
	private static class BroadcastRoutingStrategy implements RoutingStrategy {
		@Override
		public GsRoutingKey getRoutingKey(Object[] args) {
			return null; // TODO: introduce GsRoutingKey.broadcast()?
		}
	}
	
}