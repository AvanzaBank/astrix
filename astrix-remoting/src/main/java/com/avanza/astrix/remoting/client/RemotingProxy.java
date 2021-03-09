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

import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.API_VERSION;
import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.SERVICE_API;
import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.SERVICE_METHOD_SIGNATURE;

import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.DefaultTraceProvider;
import com.avanza.astrix.beans.tracing.InvocationExecutionWatcher;
import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.ReplaySubject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemotingProxy implements InvocationHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RemotingProxy.class);

	private final int apiVersion;
	private final String serviceApi;
	private final ConcurrentMap<Method, RemoteServiceMethod> remoteServiceMethodByMethod = new ConcurrentHashMap<>();
	private final ConcurrentMap<Method, List<InvocationExecutionWatcher>> invocationWatchersByMethod = new ConcurrentHashMap<>();
	private final RemoteServiceMethodFactory remoteServiceMethodFactory;
	private final ReactiveTypeConverter reactiveTypeConverter;

	/**
	 * @deprecated please use {@link #create(Class, Class, RemotingTransport, AstrixObjectSerializer, RoutingStrategy, ReactiveTypeConverter, AstrixTraceProvider)}
	 */
	@Deprecated
	public static <T> T create(Class<T> proxyApi, Class<?> targetApi, RemotingTransport transport, AstrixObjectSerializer objectSerializer, RoutingStrategy defaultRoutingStrategy, ReactiveTypeConverter reactiveTypeConverter) {
		return create(proxyApi, targetApi, transport, objectSerializer, defaultRoutingStrategy, reactiveTypeConverter, new DefaultTraceProvider());
	}

	public static <T> T create(
			Class<T> proxyApi,
			Class<?> targetApi,
			RemotingTransport transport,
			AstrixObjectSerializer objectSerializer,
			RoutingStrategy defaultRoutingStrategy,
			ReactiveTypeConverter reactiveTypeConverter,
			AstrixTraceProvider astrixTraceProvider
	) {
		RemotingProxy handler = new RemotingProxy(proxyApi, targetApi, objectSerializer, transport, defaultRoutingStrategy, reactiveTypeConverter, astrixTraceProvider);
		T serviceProxy = (T) Proxy.newProxyInstance(RemotingProxy.class.getClassLoader(), new Class[]{proxyApi}, handler);
		return serviceProxy;
	}
	
	private RemotingProxy(Class<?> proxiedServiceApi,
						  Class<?> targetServiceApi,
							    AstrixObjectSerializer objectSerializer,
							    RemotingTransport AstrixServiceTransport,
							    RoutingStrategy defaultRoutingStrategy,
							    ReactiveTypeConverter reactiveTypeConverter,
							    AstrixTraceProvider astrixTraceProvider) {
		this.reactiveTypeConverter = reactiveTypeConverter;
		this.serviceApi = targetServiceApi.getName();
		this.apiVersion = objectSerializer.version();
		RemotingEngine remotingEngine = new RemotingEngine(AstrixServiceTransport, objectSerializer, apiVersion);
		this.remoteServiceMethodFactory = new RemoteServiceMethodFactory(remotingEngine, defaultRoutingStrategy);
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
			invocationWatchersByMethod.put(proxiedMethod, astrixTraceProvider.getClientCallExecutionWatchers(serviceApi, proxiedMethod.getName()));
		}
		LOG.info("Initialized Astrix remoting client, consuming api=[{}] using proxiedApi=[{}]", targetServiceApi.getName(), proxiedServiceApi.getName());
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
		
		invocationRequest.setHeader(API_VERSION, Integer.toString(this.apiVersion));
		invocationRequest.setHeader(SERVICE_METHOD_SIGNATURE, remoteServiceMethod.getSignature());
		invocationRequest.setHeader(SERVICE_API, this.serviceApi);

		Runnable afterInvocationWatchers = InvocationExecutionWatcher.apply(invocationWatchersByMethod.get(method), invocationRequest.getHeaders());

		Observable<?> result = remoteServiceMethod.invoke(invocationRequest, args)
				.doOnError(e -> afterInvocationWatchers.run())
				.doOnCompleted(afterInvocationWatchers::run);
		if (isObservableType(method.getReturnType())) {
			return result;
		}
		if (isReactiveType(method.getReturnType())) {
			ReplaySubject<Object> subject = ReplaySubject.create();
	        // eagerly kick off subscription
	        result.subscribe(subject);
	        // return the subject that can be subscribed to later while the execution has already started
			return reactiveTypeConverter.toCustomReactiveType(method.getReturnType(), subject);
		}
		if (Future.class.equals(method.getReturnType())) {
			ReplaySubject<Object> subject = ReplaySubject.create();
	        // eagerly kick off subscription
	        result.subscribe(subject);
	        // return the subject that can be subscribed to later while the execution has already started
			return new FutureAdapter<>(subject);
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
		Class<?> returnType = method.getReturnType();
		if (isReactiveType(returnType) || isObservableType(returnType) || isFutureType(returnType)) {
			return ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0];
		}
		return method.getGenericReturnType();
	}

	private boolean isFutureType(Class<?> returnType) {
		return Future.class.isAssignableFrom(returnType);
	}

	private boolean isObservableType(Class<?> returnType) {
		return Observable.class.isAssignableFrom(returnType);
	}

	private boolean isReactiveType(Class<?> asyncType) {
		return this.reactiveTypeConverter.isReactiveType(asyncType);
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
	
}
