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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

import com.avanza.astrix.core.AstrixPartitionedRouting;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRemoteResultReducer;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class PartitionedRemoteServiceMethod implements RemoteServiceMethod {

	private final int partitionByIndex;
	private final String methodSignature;
	private final RemotingEngine remotingEngine;
	private final Type targetReturnType;
	private final PartitionContext partitionContext;

	public PartitionedRemoteServiceMethod(int partitionedByArgumentIndex,
										  Method proxiedMethod,
										  String methodSignature, 
										  RemotingEngine remotingEngine,
										  Type targetReturnType) {
		this.partitionByIndex = partitionedByArgumentIndex;
		this.methodSignature = methodSignature;
		this.remotingEngine = remotingEngine;
		this.targetReturnType = targetReturnType;
		this.partitionContext = createPartitionContext(partitionedByArgumentIndex, proxiedMethod);
	}

	private PartitionContext createPartitionContext(int partitionedByArgumentIndex, Method proxiedMethod) {
		AstrixPartitionedRouting partitionBy = getPartitionByAnnotation(proxiedMethod, partitionedByArgumentIndex);
		Class<?> partitionedArgumentType = proxiedMethod.getParameterTypes()[partitionedByArgumentIndex];
		if (partitionedArgumentType.isArray()) {
			return new PartitionContext(getReducer(partitionBy, proxiedMethod), new ArrayPartitionedArgumentType(partitionedArgumentType.getComponentType()));
		}
		Class<? extends Collection> collectionFactory = partitionBy.collectionFactory();
		if (!proxiedMethod.getParameterTypes()[partitionedByArgumentIndex].isAssignableFrom(collectionFactory)) {
			throw new IllegalArgumentException(String.format("Collection class supplied by @AstrixPartitionedRouting is not "
											 + "compatible with argument type, argumentType=%s classType=%s", 
											 proxiedMethod.getParameterTypes()[partitionedByArgumentIndex].getName(),
											 collectionFactory));
		}
		return new PartitionContext(getReducer(partitionBy, proxiedMethod), new JavaCollectionPartitionedArgumentType(collectionFactory));
	}

	private Class<? extends AstrixRemoteResultReducer> getReducer(
			AstrixPartitionedRouting partitionBy, Method targetServiceMethod) {
		Class<? extends AstrixRemoteResultReducer> reducerType = partitionBy.reducer();
		RemotingProxyUtil.validateRemoteResultReducer(targetServiceMethod, reducerType);
		return reducerType;
	}

	private static AstrixPartitionedRouting getPartitionByAnnotation(Method proxiedMethod, int partitionedByArgumentIndex) {
		for (Annotation a : proxiedMethod.getParameterAnnotations()[partitionedByArgumentIndex]) {
			if (a instanceof AstrixPartitionedRouting) {
				return AstrixPartitionedRouting.class.cast(a);
			}
		}
		throw new IllegalStateException("Programming error, proxied method does not hold AstrixPartitionedBy annotation: " + proxiedMethod);
	}

	@Override
	public String getSignature() {
		return methodSignature;
	}

	@Override
	public Observable<?> invoke(AstrixServiceInvocationRequest invocationRequest, Object[] args) throws Exception {
		/*
		 * 1. Partition Requests
		 * 2. Marshall arguments
		 * 3. Execute requests
		 */
		PartitionedRequestsBuilder requestsBuilder = new PartitionedRequestsBuilder(this.remotingEngine.partitionCount());
		Observable<AstrixServiceInvocationResponse> responses = Observable.empty();
		for (PartitionedRequest partitionRequest : requestsBuilder.partitionRequest(args)) {
			responses = responses.mergeWith(partitionRequest.submitRequest(invocationRequest, args));
		}
		return reduce(responses);
	}

	private <T> Observable<T> reduce(Observable<AstrixServiceInvocationResponse> responses) {
		final AstrixRemoteResultReducer<T, T> reducer = this.partitionContext.newRemoteResultReducer();
		return responses.toList().map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
			@Override
			public T call(List<AstrixServiceInvocationResponse> t1) {
				List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>(t1.size());
				for (AstrixServiceInvocationResponse response : t1) {
					AstrixRemoteResult<T> result = remotingEngine.toRemoteResult(response, targetReturnType);
					unmarshalledResponses.add(result);
				}
				return reducer.reduce(unmarshalledResponses);
			}
		});
		
	}

	private class PartitionedRequest {
		private final CollectionBuilder keys;
		private final RoutingKey targetPartitionRoutingKey;

		public PartitionedRequest(CollectionBuilder keys, int targetPartition) {
			this.keys = keys;
			this.targetPartitionRoutingKey = RoutingKey.create(targetPartition);
		}

		public void addKey(Object requestedKey) {
			this.keys.add(requestedKey);
		}
		
		public Observable<AstrixServiceInvocationResponse> submitRequest(AstrixServiceInvocationRequest invocationRequest, Object[] unpartitionedArguments) {
			AstrixServiceInvocationRequest partitionedRequest = new AstrixServiceInvocationRequest();
			partitionedRequest.setAllHeaders(invocationRequest.getHeaders());
			Object[] requestForPartition = Arrays.copyOf(unpartitionedArguments, unpartitionedArguments.length);
			requestForPartition[partitionByIndex] = this.keys.buildTarget();
			partitionedRequest.setArguments(remotingEngine.marshall(requestForPartition));
			return remotingEngine.processRoutedRequest(partitionedRequest, targetPartitionRoutingKey);
		}
	}
	
	private class PartitionedRequestsBuilder {
		private final PartitionedRequest[] requests;
		
		public PartitionedRequestsBuilder(int partitionCount) {
			this.requests = new PartitionedRequest[partitionCount];
		}

		public Iterable<PartitionedRequest> partitionRequest(Object[] args) {
			partitionContext.consumeAll(args[partitionByIndex], new Consumer() {
				@Override
				public void accept(Object requestedKey) {
					addKey(requestedKey);
				}
			});
			List<PartitionedRequest> result = new LinkedList<>();
			for (PartitionedRequest request : requests) {
				if (request != null) {
					result.add(request);
				}
			}
			return result;
		}

		public void addKey(Object requestedKey) {
			int targetPartition = requestedKey.hashCode() % requests.length;
			PartitionedRequest partitionRequest = this.requests[targetPartition];
			if (partitionRequest == null) {
				partitionRequest = new PartitionedRequest(newCollectionInstance(), targetPartition);
				this.requests[targetPartition] = partitionRequest;
				
			}
			partitionRequest.addKey(requestedKey);
		}

		private CollectionBuilder newCollectionInstance() {
			return partitionContext.newCollectionInstance();
		}
	}
	
	private static class PartitionContext {

		private final Class<? extends AstrixRemoteResultReducer> remoteResultReducerFactory;
		private final PartitionedArgumentType partitionedArgumentType;
		
		public PartitionContext(Class<? extends AstrixRemoteResultReducer> reducerFactory, PartitionedArgumentType partitionedArgumentType) {
			this.remoteResultReducerFactory = reducerFactory;
			this.partitionedArgumentType = partitionedArgumentType;
		}
		
		public void consumeAll(Object object, Consumer consumer) {
			this.partitionedArgumentType.consumeAll(object, consumer);
		}

		@SuppressWarnings("unchecked")
		public <T> AstrixRemoteResultReducer<T, T> newRemoteResultReducer() {
			return ReflectionUtil.newInstance(remoteResultReducerFactory);
		}

		private CollectionBuilder newCollectionInstance() {
			return partitionedArgumentType.newInstance();
		}
	}
	
	private interface PartitionedArgumentType {
		CollectionBuilder newInstance();
		<E> void consumeAll(Object argument, Consumer consumer);
	}
	
	interface Consumer {
		void accept(Object element);
	}
	
	
	private static class JavaCollectionPartitionedArgumentType implements PartitionedArgumentType {
		private final Class<? extends Collection> collectionFactory;
		
		public JavaCollectionPartitionedArgumentType(Class<? extends Collection> collectionFactory) {
			this.collectionFactory = collectionFactory;
		}

		@SuppressWarnings("unchecked")
		@Override
		public CollectionBuilder newInstance() {
			return new JavaCollectionBuilder(ReflectionUtil.newInstance(this.collectionFactory));
		}

		@Override
		public void consumeAll(Object argument, Consumer consumer) {
			for (Object element : (Collection) argument) {
				consumer.accept(element);
			}
		}
	}
	
	private static class ArrayPartitionedArgumentType implements PartitionedArgumentType {
		
		private Class<?> elementType;
		
		public ArrayPartitionedArgumentType(Class<?> elementType) {
			this.elementType = elementType;
		}

		@Override
		public CollectionBuilder newInstance() {
			return new ArrayCollectionBuilder(elementType);
		}

		@Override
		public void consumeAll(Object argument, Consumer consumer) {
			for (int i = 0; i < Array.getLength(argument); i++) {
				consumer.accept(Array.get(argument, i));
			}
		}
	}
	
	private static class ArrayCollectionBuilder extends CollectionBuilder {
		
		private final Class<?> elementType;
		private final List<Object> elements = new ArrayList<>();
		
		public ArrayCollectionBuilder(Class<?> elementType) {
			this.elementType = elementType;
		}
		
		@Override
		void add(Object element) {
			elements.add(element);
		}
		
		@Override
		Object buildTarget() {
			Object array = initArray(elements.size());
			int nextIndex = 0;
			for (Object element : elements) {
				Array.set(array, nextIndex, element);
				nextIndex++;
			}
			return array;
		}
		
		protected Object initArray(int size) {
			return Array.newInstance(elementType, size);
		}
	}
	
	private static abstract class CollectionBuilder {
		
		abstract void add(Object element);
		
		abstract Object buildTarget();
	}
	
	private static class JavaCollectionBuilder extends CollectionBuilder {
		private Collection<Object> collection;

		public JavaCollectionBuilder(Collection<Object> collection) {
			this.collection = collection;
		}
		
		@Override
		void add(Object element) {
			this.collection.add(element);
		}
		
		@Override
		Object buildTarget() {
			return collection;
		}
	}
	
}
