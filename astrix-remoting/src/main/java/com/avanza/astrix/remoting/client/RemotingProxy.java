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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import rx.Observable;

import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemotingProxy implements InvocationHandler {
	
	private final int apiVersion;
	private final String serviceApi;
	private final ConcurrentMap<Method, RemoteServiceMethod> remoteServiceMethodByMethod = new ConcurrentHashMap<>();
	private final boolean isObservableApi;
	private final boolean isAsyncApi;
	private final RemoteServiceMethodFactory remoteServiceMethodFactory;

	public static <T> T create(Class<T> service, RemotingTransport transport, AstrixObjectSerializer objectSerializer, RoutingStrategy routingStrategy) {
		RemotingProxy handler = new RemotingProxy(service, objectSerializer, transport, routingStrategy);
		T serviceProxy = (T) Proxy.newProxyInstance(RemotingProxy.class.getClassLoader(), new Class[]{service}, handler);
		return serviceProxy;
	}
	
	private RemotingProxy(Class<?> proxiedServiceApi,
							    AstrixObjectSerializer objectSerializer,
							    RemotingTransport AstrixServiceTransport,
							    RoutingStrategy routingStrategy) {
		this.apiVersion = objectSerializer.version();
		RemotingEngine remotingEngine = new RemotingEngine(AstrixServiceTransport, objectSerializer, apiVersion);
		this.remoteServiceMethodFactory = new RemoteServiceMethodFactory(remotingEngine, routingStrategy);
		if (proxiedServiceApi.getSimpleName().startsWith("Observable")) {
			String packageAndEventualOuterClassName = proxiedServiceApi.getName().substring(0, proxiedServiceApi.getName().length() - proxiedServiceApi.getSimpleName().length());
			String serviceSimpleName = proxiedServiceApi.getSimpleName().substring("Observable".length());
			String serviceClassName = packageAndEventualOuterClassName + serviceSimpleName;
			this.serviceApi = serviceClassName; 
			this.isObservableApi = true;
			this.isAsyncApi = false;
		} else if (proxiedServiceApi.getSimpleName().endsWith("Async")) {
			String serviceClassName = proxiedServiceApi.getName().substring(0, proxiedServiceApi.getName().length() - "Async".length());
			this.serviceApi = serviceClassName; 
			this.isObservableApi = false;
			this.isAsyncApi = true; 
		} else {
			this.serviceApi = proxiedServiceApi.getName();
			this.isObservableApi = false;
			this.isAsyncApi = false;
		}
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
			return method.invoke(this, args);
		}
		RemoteServiceMethod remoteServiceMethod = this.remoteServiceMethodByMethod.get(method);
		
		AstrixServiceInvocationRequest invocationRequest = new AstrixServiceInvocationRequest();
		
		invocationRequest.setHeader("apiVersion", Integer.toString(this.apiVersion));
		invocationRequest.setHeader("serviceMethodSignature", remoteServiceMethod.getSignature());
		invocationRequest.setHeader("serviceApi", this.serviceApi);
		
		Observable<?> result = remoteServiceMethod.invoke(invocationRequest, args);
		if (isObservableApi) {
			return result;
		}
		if (isAsyncApi) {
			return new FutureAdapter<>(result);
		}
		return result.toBlocking().first();
	}
	
	private Type getReturnType(Method method) {
		if (isObservableApi || isAsyncApi) {
			return ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0];
		}
		return method.getGenericReturnType();
	}
	
}
