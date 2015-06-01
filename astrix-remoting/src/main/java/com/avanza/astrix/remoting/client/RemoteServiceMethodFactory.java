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

import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixPartitionedRouting;
import com.avanza.astrix.core.RemoteResultReducer;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemoteServiceMethodFactory {

	private final RemotingEngine remotingEngine;
	private final RoutingStrategy routingStrategy;
	
	public RemoteServiceMethodFactory(RemotingEngine remotingEngine,
			RoutingStrategy routingStrategy) {
		this.remotingEngine = remotingEngine;
		this.routingStrategy = routingStrategy;
	}

	public RemoteServiceMethod createRemoteServiceMethod(
			Class<?> targetServiceType, Method proxiedMethod, Type targetReturnType) {
		String methodSignature = ReflectionUtil.methodSignatureWithoutReturnType(proxiedMethod);
		if (proxiedMethod.isAnnotationPresent(AstrixBroadcast.class)) {
			return new BroadcastedRemoteServiceMethod(methodSignature,
					getRemoteResultReducerClass(proxiedMethod, targetServiceType),
					remotingEngine, targetReturnType);
		}
		int partitionedByArgumentIndex = getPartitionedByAnnotation(proxiedMethod);
		if (partitionedByArgumentIndex >= 0) {
			return new PartitionedRemoteServiceMethod(partitionedByArgumentIndex, proxiedMethod, methodSignature, remotingEngine, targetReturnType);
		}
		return new RoutedRemoteServiceMethod(methodSignature, routingStrategy.create(proxiedMethod), remotingEngine, targetReturnType);
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

	private Class<? extends RemoteResultReducer<?>> getRemoteResultReducerClass(
			Method proxyServiceMethod, Class<?> targetServiceType) {
		Method targetServiceMethod = ReflectionUtil.getMethod(targetServiceType, proxyServiceMethod.getName(),proxyServiceMethod.getParameterTypes()); 
		AstrixBroadcast broadcast = targetServiceMethod
				.getAnnotation(AstrixBroadcast.class);
		Class<? extends RemoteResultReducer<?>> reducerType = (Class<? extends RemoteResultReducer<?>>) broadcast.reducer();
		RemotingProxyUtil.validateRemoteResultReducer(targetServiceMethod, reducerType);
		return (Class<? extends RemoteResultReducer<?>>) reducerType;
	}

}
