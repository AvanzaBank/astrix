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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRemoteResultReducer;
import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceInvocationException;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.remoting.util.MethodSignatureBuilder;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixRemotingProxy implements InvocationHandler {
	
	private final int apiVersion;
	private final String serviceApi;
	private final AstrixObjectSerializer objectSerializer;
	private final AstrixRemotingTransport serviceTransport;
	private final ConcurrentMap<Method, RemoteServiceMethod> remoteServiceMethodByMethod = new ConcurrentHashMap<>();
	private final boolean isObservableApi;
	private final boolean isAsyncApi;
	
	private AstrixRemotingProxy(Class<?> proxiedServiceApi,
							  AstrixObjectSerializer objectSerializer,
							  AstrixRemotingTransport AstrixServiceTransport,
							  RoutingStrategy routingStrategy) {
		this.apiVersion = objectSerializer.version();
		this.objectSerializer = objectSerializer;
		this.serviceTransport = AstrixServiceTransport;
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
		for (Method m : proxiedServiceApi.getMethods()) {
			if (m.isAnnotationPresent(AstrixBroadcast.class)) {
				remoteServiceMethodByMethod.put(m, new RemoteServiceMethod(MethodSignatureBuilder.build(m), getRemoteResultReducerClass(m, targetServiceType)));
			} else {
				remoteServiceMethodByMethod.put(m, new RemoteServiceMethod(MethodSignatureBuilder.build(m), routingStrategy.create(m)));
			}
		}
	}

	private Class<? extends AstrixRemoteResultReducer<?, ?>> getRemoteResultReducerClass(Method proxyServiceMethod, Class<?> targetServiceType) {
		Method targetServiceMethod = ReflectionUtil.getMethod(targetServiceType, proxyServiceMethod.getName(), proxyServiceMethod.getParameterTypes());
		AstrixBroadcast broadcast = targetServiceMethod.getAnnotation(AstrixBroadcast.class);
		Class<? extends AstrixRemoteResultReducer> reducerType = broadcast.reducer();
		validateRemoteResultReducerReturnType(targetServiceMethod, reducerType);
		validateRemoteResultReducerArgumentType(targetServiceMethod, reducerType);
		return (Class<? extends AstrixRemoteResultReducer<?, ?>>) reducerType;
	}

	private void validateRemoteResultReducerReturnType(Method m,
			Class<? extends AstrixRemoteResultReducer> reducerType) {
		Method reduceMethod = ReflectionUtil.getMethod(reducerType, "reduce", List.class);
		Class<?> returnType = m.getReturnType();
		if (returnType.equals(Void.TYPE)) {
			return;
		}
		if (Future.class.isAssignableFrom(returnType)) {
			ParameterizedType futureType = ParameterizedType.class.cast(m.getGenericReturnType());
			returnType = (Class<?>) (futureType.getActualTypeArguments()[0]);
		}
		if (!returnType.isAssignableFrom(reduceMethod.getReturnType())) {
			throw new IncompatibleRemoteResultReducerException(
					String.format("Return type of AstrixRemoteResultReducer must be same as (or subtype) of the one returned by the service method. "
								+ "serviceMethod=%s reducerType=%s"
							    , m, reducerType)); 
		}
	}

	private void validateRemoteResultReducerArgumentType(Method m, 
														 Class<? extends AstrixRemoteResultReducer> reducerType) {
		// Lookup the "<T>" type parameter in: "R reduce(List<AstrixRemoteResult<T>> result)";
		Method reduceMethod = ReflectionUtil.getMethod(reducerType, "reduce", List.class);
		ParameterizedType listType = (ParameterizedType) reduceMethod.getGenericParameterTypes()[0];
		ParameterizedType astrixRemoteResultType = (ParameterizedType) listType.getActualTypeArguments()[0];
		Type astrixRemoteResultTypeParameter = astrixRemoteResultType.getActualTypeArguments()[0];
		if (!(astrixRemoteResultTypeParameter instanceof Class)) {
			return;
		}
		Class<?> type = (Class<?>) astrixRemoteResultTypeParameter;
		if (!type.isAssignableFrom(m.getReturnType()) && !m.getReturnType().equals(Void.TYPE)) {
			throw new IncompatibleRemoteResultReducerException(
					String.format("Generic argument type of AstrixRemoteResultReducer.reduce(List<AstrixRemoteResult<T>>) must same as of the one returned by the serivce method. "
							+ "serviceMethod=%s reducerType=%s"
							, m, reducerType)); 
		}
	}
	
	public static <T> T create(Class<T> service, AstrixRemotingTransport transport, AstrixObjectSerializer objectSerializer, RoutingStrategy routingStrategy) {
		AstrixRemotingProxy handler = new AstrixRemotingProxy(service, objectSerializer, transport, routingStrategy);
		T serviceProxy = (T) Proxy.newProxyInstance(AstrixRemotingProxy.class.getClassLoader(), new Class[]{service}, handler);
		return serviceProxy;
	}
	
	@Override
	public String toString() {
		return "AstrixRemotingProxy[" + this.serviceApi + "]";
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().equals(Object.class)) {
			return method.invoke(this, args);
		}
		RemoteServiceMethod serviceMethod = this.remoteServiceMethodByMethod.get(method);
		
		Type returnType = getReturnType(method);
		AstrixServiceInvocationRequest invocationRequest = new AstrixServiceInvocationRequest();
		
		invocationRequest.setHeader("apiVersion", Integer.toString(this.apiVersion));
		String methodSignature = serviceMethod.getSignature();
		
		invocationRequest.setHeader("serviceMethodSignature", methodSignature);
		invocationRequest.setHeader("serviceApi", getTargetServiceName());
		invocationRequest.setArguments(marshall(args));
		
		Observable<?> result;
		if (serviceMethod.isBroadcast()) {
			result = observeProcessBroadcastRequest(returnType, invocationRequest, serviceMethod);
		} else {
			RoutingKey routingKey = serviceMethod.getRoutingKey(args);
			if (routingKey == null) {
				throw new IllegalStateException(String.format("Service method is routed but the defined remotingKey value was null: method=%s", method.toString()));
			}
			result = observeProcessRoutedRequest(returnType, invocationRequest, routingKey);
		}
		if (isObservableApi) {
			return result;
		}
		if (isAsyncApi) {
			return new FutureAdapter<>(result);
		}
		return result.toBlocking().first();
	}
	
	private static class FutureAdapter<T> implements Future<T> {
		
		private final CountDownLatch done = new CountDownLatch(1);
		private volatile T result;
		private volatile Throwable exception;
		
		public FutureAdapter(Observable<T> obs) {
			obs.subscribe(new Action1<T>() {
				@Override
				public void call(T t1) {
					result = t1;
					done.countDown();
				}
			}, new Action1<Throwable>() {

				@Override
				public void call(Throwable t1) {
					exception = t1;
					done.countDown();
				}
				
			});
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}
		
		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return done.getCount() == 0;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			done.await();
			return getResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if (!done.await(timeout, unit)) {
				throw new TimeoutException();
			}
			return getResult();
		}

		private T getResult() throws ExecutionException {
			if (exception != null) {
				throw new ExecutionException(exception);
			}
			return result;		
		}
		
	}

	private Type getReturnType(Method method) {
		if (isObservableApi || isAsyncApi) {
			return ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0];
		}
		return method.getGenericReturnType();
	}

	private String getTargetServiceName() {
		return this.serviceApi;
	}

	private <T> Observable<T> observeProcessBroadcastRequest(
			final Type returnType, 
			AstrixServiceInvocationRequest request,
			RemoteServiceMethod serviceMethod) throws InstantiationException,
			IllegalAccessException {
		final AstrixRemoteResultReducer<T, T> reducer = (AstrixRemoteResultReducer<T, T>) serviceMethod.newReducer();
		Observable<List<AstrixServiceInvocationResponse>> responesObservable = this.serviceTransport.observeProcessBroadcastRequest(request);
		if (returnType.equals(Void.TYPE)) {
			return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
				@Override
				public T call(List<AstrixServiceInvocationResponse> t1) {
					return null;
				}
			});
		}
		return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
			@Override
			public T call(List<AstrixServiceInvocationResponse> t1) {
				List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>();
				for (AstrixServiceInvocationResponse response : t1) {
					AstrixRemoteResult<T> result = toRemoteResult(response, returnType, apiVersion);
					unmarshalledResponses.add(result);
				}
				return reducer.reduce(unmarshalledResponses);
			}
		});
	}

	private Observable<Object> observeProcessRoutedRequest(
			final Type returnType, AstrixServiceInvocationRequest request,
			RoutingKey routingKey) {
		Observable<AstrixServiceInvocationResponse> response = this.serviceTransport.observeProcessRoutedRequest(request, routingKey);
		return response.map(new Func1<AstrixServiceInvocationResponse, Object>() {
			@Override
			public Object call(AstrixServiceInvocationResponse t1) {
				return toRemoteResult(t1, returnType, apiVersion).getResult();
			}
		});
	}
	
	private Object[] marshall(Object[] elements) {
		if (elements == null) {
			// No argument method
			return new Object[0];
		}
		Object[] result = new Object[elements.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = this.objectSerializer.serialize(elements[i], apiVersion);
		}
		return result;
	}

	private <T> AstrixRemoteResult<T> toRemoteResult(AstrixServiceInvocationResponse response, Type returnType, int version) {
		if (response.hasThrownException()) {
			return AstrixRemoteResult.failure(createClientSideException(response, version));
		}
		if (returnType.equals(Void.TYPE)) {
			return AstrixRemoteResult.voidResult();
		}
		T result = unmarshall(response, returnType, version);
		return AstrixRemoteResult.successful(result);
	}

	private <T> T unmarshall(AstrixServiceInvocationResponse response, Type returnType, int version) {
		return objectSerializer.deserialize(response.getResponseBody(), returnType, version);
	}
	
	public ServiceInvocationException createClientSideException(AstrixServiceInvocationResponse response, int version) {
		if (response.getException() != null) {
			ServiceInvocationException exception = objectSerializer.deserialize(response.getException(), 
																		ServiceInvocationException.class, 
																		version);
			return exception.reCreateOnClientSide(CorrelationId.valueOf(response.getCorrelationId()));
		}
		return new RemoteServiceInvocationException(response.getExceptionMsg(), response.getThrownExceptionType(), CorrelationId.valueOf(response.getCorrelationId()));
	}
	
}
