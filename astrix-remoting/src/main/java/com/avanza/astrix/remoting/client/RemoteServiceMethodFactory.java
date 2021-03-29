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
package com.avanza.astrix.remoting.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.avanza.astrix.core.*;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemoteServiceMethodFactory {

	private final RemotingEngine remotingEngine;
	private final RoutingStrategy defaultRoutingStrategy;
	
	public RemoteServiceMethodFactory(RemotingEngine remotingEngine,
			RoutingStrategy defaultRoutingStrategy) {
		this.remotingEngine = remotingEngine;
		this.defaultRoutingStrategy = defaultRoutingStrategy;
	}

	public RemoteServiceMethod createRemoteServiceMethod(
			Class<?> targetServiceType, Method proxiedMethod, Type targetReturnType) {
		String methodSignature = ReflectionUtil.methodSignatureWithoutReturnType(proxiedMethod);
		if (proxiedMethod.isAnnotationPresent(AstrixBroadcast.class)) {
			Method targetServiceMethod = ReflectionUtil.getMethod(targetServiceType, proxiedMethod.getName(),proxiedMethod.getParameterTypes());
			return new BroadcastedRemoteServiceMethod(methodSignature,
					getRemoteResultReducerClass(targetServiceMethod),
					remotingEngine, targetReturnType);
		}
		int partitionedByArgumentIndex = getPartitionedByAnnotation(proxiedMethod);
		if (partitionedByArgumentIndex >= 0) {
			Method targetServiceMethod = ReflectionUtil.getMethod(targetServiceType, proxiedMethod.getName(),proxiedMethod.getParameterTypes());
			return new PartitionedRemoteServiceMethod(partitionedByArgumentIndex, proxiedMethod, methodSignature, remotingEngine, targetReturnType, targetServiceMethod);
		}
		if (proxiedMethod.isAnnotationPresent(AstrixRoutingStrategy.class)) {
			RoutingStrategy routingStrategy = createRoutingStrategy(proxiedMethod);
			return new RoutedRemoteServiceMethod(methodSignature, routingStrategy.create(proxiedMethod), remotingEngine, targetReturnType);
		}
		return new RoutedRemoteServiceMethod(methodSignature, defaultRoutingStrategy.create(proxiedMethod), remotingEngine, targetReturnType);
	}

	private RoutingStrategy createRoutingStrategy(Method proxiedMethod) {
		AstrixRoutingStrategy router = proxiedMethod.getAnnotation(AstrixRoutingStrategy.class);
		Class<? extends RoutingStrategy> routingStrategyClass = router.value();
		try {
			return ReflectionUtil.newInstance(routingStrategyClass);
		} catch (Exception e) {
			throw new IllegalServiceMetadataException("Failed to create RoutingStrategy", e);
		}
	}
	
	public static int getPartitionedByAnnotation(Method m) {
		int partitionedByIndex = -1;
		for (int argumentIndex = 0; argumentIndex < m.getParameterTypes().length; argumentIndex++) {
			for (Annotation a : m.getParameterAnnotations()[argumentIndex]) {
				if (!a.annotationType().equals(AstrixPartitionedRouting.class)) {
					continue;
				}
				if (partitionedByIndex >= 0) {
					throw new IllegalArgumentException("Ambigous service method signature. Multiple AstrixPartitionedBy annotations found: " + m.toString());
				}
				partitionedByIndex = argumentIndex;
			}
		}
		return partitionedByIndex;
	}

	private Class<? extends RemoteResultReducer<?>> getRemoteResultReducerClass( Method targetServiceMethod) {
		AstrixBroadcast broadcast = targetServiceMethod.getAnnotation(AstrixBroadcast.class);
		@SuppressWarnings("unchecked")
		Class<? extends RemoteResultReducer<?>> reducerType = (Class<? extends RemoteResultReducer<?>>) broadcast.reducer();
		RemotingProxyUtil.validateRemoteResultReducer(targetServiceMethod, reducerType);
		return reducerType;
	}

}
