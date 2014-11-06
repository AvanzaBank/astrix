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
import se.avanzabank.asterix.core.AsterixBroadcast;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.core.AsterixRemoteResult;
import se.avanzabank.asterix.core.AsterixRemoteResultReducer;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixRemotingProxy implements InvocationHandler {
	
	private final int apiVersion;
	private final String serviceApi;
	private final AsterixObjectSerializer objectSerializer;
	private final AsterixRemotingTransport serviceTransport;
	private final ConcurrentMap<Method, RemoteServiceMethod> remoteServiceMethodByMethod = new ConcurrentHashMap<>();
	private final boolean isObservableApi;
	private final boolean isAsyncApi;
	
	private AsterixRemotingProxy(Class<?> serviceApi,
							  AsterixObjectSerializer objectSerializer,
							  AsterixRemotingTransport asterixServiceTransport) {
		this.apiVersion = objectSerializer.version();
		this.objectSerializer = objectSerializer;
		this.serviceTransport = asterixServiceTransport;
		for (Method m : serviceApi.getMethods()) {
			remoteServiceMethodByMethod.put(m, RemoteServiceMethod.create(m));
		}
		if (serviceApi.getSimpleName().startsWith("Observable")) {
			String packageAndEventualOuterClassName = serviceApi.getName().substring(0, serviceApi.getName().length() - serviceApi.getSimpleName().length());
			String serviceSimpleName = serviceApi.getSimpleName().substring("Observable".length());
			String serviceClassName = packageAndEventualOuterClassName + serviceSimpleName;
			this.serviceApi = serviceClassName; // TODO: no need to load class 
			this.isObservableApi = true;
			this.isAsyncApi = false;
		} else if (serviceApi.getSimpleName().endsWith("Async")) {
			String serviceClassName = serviceApi.getName().substring(0, serviceApi.getName().length() - "Async".length());
			this.serviceApi = serviceClassName; // TODO: no need to load class 
			this.isObservableApi = false;
			this.isAsyncApi = true;	 // TODO: introduce proxy hiearchy for different proxy types(async/observable/sync) 
		} else {
			this.serviceApi = serviceApi.getName();
			this.isObservableApi = false;
			this.isAsyncApi = false;
		}
	}

	public static <T> T create(Class<T> service, AsterixRemotingTransport transport, AsterixObjectSerializer objectSerializer) {
		AsterixRemotingProxy handler = new AsterixRemotingProxy(service, objectSerializer, transport);
		T serviceProxy = (T) Proxy.newProxyInstance(AsterixRemotingProxy.class.getClassLoader(), new Class[]{service}, handler);
		return serviceProxy;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO: validate method signature when creating proxy rather than on each invocation
		RemoteServiceMethod serviceMethod = this.remoteServiceMethodByMethod.get(method);
		GsRoutingKey routingKey = serviceMethod.getRoutingKey(args);
		
		Type returnType = getReturnType(method);
		AsterixServiceInvocationRequest invocationRequest = new AsterixServiceInvocationRequest();
		
		invocationRequest.setHeader("apiVersion", Integer.toString(this.apiVersion));
		String methodSignature = serviceMethod.getSignature();
		
		invocationRequest.setHeader("serviceMethodSignature", methodSignature);
		invocationRequest.setHeader("serviceApi", getTargetServiceName());
		invocationRequest.setArguments(marshall(args)); // TODO: support multiple arguments
		
		if (routingKey == null) {
			Observable<?> result = observeProcessBroadcastRequest(returnType, invocationRequest, method);
			if (isObservableApi) {
				return result;
			}
			if (isAsyncApi) {
				return new FutureAdapter<>(result);
			}
			return result.toBlockingObservable().first();
		} else {
			Observable<Object> result = observeProcessRoutedRequest(returnType, invocationRequest, routingKey);
			if (isObservableApi) {
				return result;
			}
			if (isAsyncApi) {
				return new FutureAdapter<>(result);
			}
			return result.toBlockingObservable().first();
		}
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
			final Type returnType, AsterixServiceInvocationRequest request,
			Method method) throws InstantiationException,
			IllegalAccessException {
		AsterixBroadcast broadcast = method.getAnnotation(AsterixBroadcast.class);
		if (broadcast == null) {
			throw new IllegalStateException(String.format("Method %s uses broadcast but has not defined an reducer using AsterixBroadcast", method.toString()));
		}
		final AsterixRemoteResultReducer<T, T> reducer = (AsterixRemoteResultReducer<T, T>) broadcast.reducer().newInstance();
		Observable<List<AsterixServiceInvocationResponse>> responesObservable = this.serviceTransport.observeProcessBroadcastRequest(request);
//		if (returnType.equals(Void.TYPE)) {
//			return responesObservable.map(new Func1<List<AsterixServiceInvocationResponse>, T>() {
//				@Override
//				public T call(List<AsterixServiceInvocationResponse> t1) {
//					return null; // TODO: How to handle void?
//				}
//			});
//		}
		return responesObservable.map(new Func1<List<AsterixServiceInvocationResponse>, T>() {
			@Override
			public T call(List<AsterixServiceInvocationResponse> t1) {
				List<AsterixRemoteResult<T>> unmarshalledResponses = new ArrayList<>();
				for (AsterixServiceInvocationResponse response : t1) {
					AsterixRemoteResult<T> result = toRemoteResult(response, returnType, apiVersion);
					unmarshalledResponses.add(result);
				}
				return reducer.reduce(unmarshalledResponses);
			}
		});
	}

	private Observable<Object> observeProcessRoutedRequest(
			final Type returnType, AsterixServiceInvocationRequest request,
			GsRoutingKey routingKey) {
		Observable<AsterixServiceInvocationResponse> response = this.serviceTransport.observeProcessRoutedRequest(request, routingKey);
		return response.map(new Func1<AsterixServiceInvocationResponse, Object>() {
			@Override
			public Object call(AsterixServiceInvocationResponse t1) {
				return toRemoteResult(t1, returnType, apiVersion).getResult();
			}
		});
	}
	
	private Object[] marshall(Object[] elements) {
		Object[] result = new Object[elements.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = this.objectSerializer.serialize(elements[i], apiVersion);
		}
		return result;
	}

	private <T> AsterixRemoteResult<T> toRemoteResult(AsterixServiceInvocationResponse response, Type returnType, int version) {
		if (response.hasThrownException()) {
			return AsterixRemoteResult.failure(new AsterixRemoteServiceException(response.getExceptionMsg(), response.getThrownExceptionType(), response.getCorrelationId()));
		}
		if (returnType.equals(Void.TYPE)) {
			return AsterixRemoteResult.voidResult();
		}
		T result = unmarshall(response, returnType, version);
		return AsterixRemoteResult.successful(result);
	}

	private <T> T unmarshall(AsterixServiceInvocationResponse response, Type returnType, int version) {
		return objectSerializer.deserialize(response.getResponseBody(), returnType, version);
	}

	
}
