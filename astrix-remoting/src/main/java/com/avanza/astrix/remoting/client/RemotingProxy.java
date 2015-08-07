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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import rx.Observable;

import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemotingProxy implements InvocationHandler {
	
	private final int apiVersion;
	private final String serviceApi;
	private final ConcurrentMap<Method, RemoteServiceMethod> remoteServiceMethodByMethod = new ConcurrentHashMap<>();
	private final RemoteServiceMethodFactory remoteServiceMethodFactory;

	public static <T> T create(Class<T> proxyApi, Class<?> targetApi, RemotingTransport transport, AstrixObjectSerializer objectSerializer, RoutingStrategy routingStrategy) {
		RemotingProxy handler = new RemotingProxy(proxyApi, targetApi, objectSerializer, transport, routingStrategy);
		T serviceProxy = (T) Proxy.newProxyInstance(RemotingProxy.class.getClassLoader(), new Class[]{proxyApi}, handler);
		return serviceProxy;
	}
	
	private RemotingProxy(Class<?> proxiedServiceApi,
						  Class<?> targetServiceApi,
							    AstrixObjectSerializer objectSerializer,
							    RemotingTransport AstrixServiceTransport,
							    RoutingStrategy routingStrategy) {
		this.serviceApi = targetServiceApi.getName();
		this.apiVersion = objectSerializer.version();
		RemotingEngine remotingEngine = new RemotingEngine(AstrixServiceTransport, objectSerializer, apiVersion);
		this.remoteServiceMethodFactory = new RemoteServiceMethodFactory(remotingEngine, routingStrategy);
		/*
		 * For each of the following services the "targetServiceType" resolves to MyService:
		 *  - MyService
		 *  - MyServiceAsync
		 *  - ObservableMyService
		 */
		Class<?> targetServiceType = ReflectionUtil.classForName(this.serviceApi);
		for (Method proxiedMethod : proxiedServiceApi.getMethods()) {
			Type returnType = getReturnType(proxiedMethod);
			RemoteServiceMethod remoteServiceMethod = this.remoteServiceMethodFactory.createRemoteServiceMethod(targetServiceType, proxiedMethod, returnType);
			remoteServiceMethodByMethod.put(proxiedMethod, remoteServiceMethod);
		}
	}

	@Override
	public String toString() {
		return "RemotingProxy[" + this.serviceApi + "]";
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().equals(Object.class)) {
			return ReflectionUtil.invokeMethod(method, this, args);
		}
		RemoteServiceMethod remoteServiceMethod = this.remoteServiceMethodByMethod.get(method);
		
		AstrixServiceInvocationRequest invocationRequest = new AstrixServiceInvocationRequest();
		
		invocationRequest.setHeader("apiVersion", Integer.toString(this.apiVersion));
		invocationRequest.setHeader("serviceMethodSignature", remoteServiceMethod.getSignature());
		invocationRequest.setHeader("serviceApi", this.serviceApi);
		
		Observable<?> result = remoteServiceMethod.invoke(invocationRequest, args);
		if (isObservableType(method.getReturnType())) {
			return result;
		}
		if (isFutureType(method.getReturnType())) {
			return new FutureAdapter<>(result);
		}
		try {
			return result.toBlocking().first();
		} catch (Exception e) {
			// Append invocation call stack
			appendStackTrace(e, new AstrixCallStackTrace());
			throw e;
		}
	}

	private static void appendStackTrace(Throwable exception, AstrixCallStackTrace trace) {
		Throwable lastThowableInChain = exception;
		while (lastThowableInChain.getCause() != null) {
			lastThowableInChain = lastThowableInChain.getCause();
		}
		lastThowableInChain.initCause(trace);
	}
	
	private Type getReturnType(Method method) {
		if (isObservableOrFutureType(method.getReturnType())) {
			return ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0];
		}
		return method.getGenericReturnType();
	}

	private boolean isObservableOrFutureType(Class<?> returnType) {
		return isFutureType(returnType) || isObservableType(returnType);
	}

	private boolean isObservableType(Class<?> returnType) {
		return Observable.class.isAssignableFrom(returnType);
	}

	private boolean isFutureType(Class<?> returnType) {
		return Future.class.isAssignableFrom(returnType);
	}
	
}
