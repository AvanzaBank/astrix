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
import se.avanzabank.asterix.core.AstrixBroadcast;
import se.avanzabank.asterix.core.AstrixObjectSerializer;
import se.avanzabank.asterix.core.AstrixRemoteResult;
import se.avanzabank.asterix.core.AstrixRemoteResultReducer;
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
	
	private AstrixRemotingProxy(Class<?> serviceApi,
							  AstrixObjectSerializer objectSerializer,
							  AstrixRemotingTransport astrixServiceTransport) {
		this.apiVersion = objectSerializer.version();
		this.objectSerializer = objectSerializer;
		this.serviceTransport = astrixServiceTransport;
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

	public static <T> T create(Class<T> service, AstrixRemotingTransport transport, AstrixObjectSerializer objectSerializer) {
		AstrixRemotingProxy handler = new AstrixRemotingProxy(service, objectSerializer, transport);
		T serviceProxy = (T) Proxy.newProxyInstance(AstrixRemotingProxy.class.getClassLoader(), new Class[]{service}, handler);
		return serviceProxy;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO: validate method signature when creating proxy rather than on each invocation
		RemoteServiceMethod serviceMethod = this.remoteServiceMethodByMethod.get(method);
		GsRoutingKey routingKey = serviceMethod.getRoutingKey(args);
		
		Class<?> returnType = getReturnType(method);
		AstrixServiceInvocationRequest invocationRequest = new AstrixServiceInvocationRequest();
		
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

	private Class<?> getReturnType(Method method) {
		if (isObservableApi || isAsyncApi) {
			return (Class<?>)(ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0]);
		}
		Class<?> returnType = method.getReturnType();
		return returnType;
	}

	private String getTargetServiceName() {
		return this.serviceApi;
	}

	private <T> Observable<T> observeProcessBroadcastRequest(
			final Class<T> returnType, AstrixServiceInvocationRequest request,
			Method method) throws InstantiationException,
			IllegalAccessException {
		AstrixBroadcast broadcast = method.getAnnotation(AstrixBroadcast.class);
		if (broadcast == null) {
			throw new IllegalStateException(String.format("Method %s uses broadcast but has not defined an reducer using AstrixBroadcast", method.toString()));
		}
		final AstrixRemoteResultReducer<T, T> reducer = (AstrixRemoteResultReducer<T, T>) broadcast.reducer().newInstance();
		Observable<List<AstrixServiceInvocationResponse>> responesObservable = this.serviceTransport.observeProcessBroadcastRequest(request);
//		if (returnType.equals(Void.TYPE)) {
//			return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
//				@Override
//				public T call(List<AstrixServiceInvocationResponse> t1) {
//					return null; // TODO: How to handle void?
//				}
//			});
//		}
		return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
			@Override
			public T call(List<AstrixServiceInvocationResponse> t1) {
				List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>();
				for (AstrixServiceInvocationResponse response : t1) {
					unmarshalledResponses.add(toRemoteResult(response, returnType, apiVersion));
				}
				return reducer.reduce(unmarshalledResponses);
			}
		});
	}

	private Observable<Object> observeProcessRoutedRequest(
			final Class<?> returnType, AstrixServiceInvocationRequest request,
			GsRoutingKey routingKey) {
		Observable<AstrixServiceInvocationResponse> response = this.serviceTransport.observeProcessRoutedRequest(request, routingKey);
		return response.map(new Func1<AstrixServiceInvocationResponse, Object>() {
			@Override
			public Object call(AstrixServiceInvocationResponse t1) {
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

	private <T> AstrixRemoteResult<T> toRemoteResult(AstrixServiceInvocationResponse response, Class<T> returnType, int version) {
		if (response.hasThrownException()) {
			return AstrixRemoteResult.failure(new AstrixRemoteServiceException(response.getExceptionMsg(), response.getThrownExceptionType(), response.getCorrelationId()));
		}
		if (returnType.equals(Void.TYPE)) {
			return AstrixRemoteResult.voidResult();
		}
		return AstrixRemoteResult.successful(unmarshall(response, returnType, version));
	}

	private <T> T unmarshall(AstrixServiceInvocationResponse response, Class<T> returnType, int version) {
		return objectSerializer.deserialize(response.getResponseBody(), returnType, version);
	}

	
}
