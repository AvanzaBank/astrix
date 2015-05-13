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

	private final int partitionedArgumentIndex;
	private final String methodSignature;
	private final RemotingEngine remotingEngine;
	private final Type targetReturnType;
	private final Class<? extends AstrixRemoteResultReducer<?,?>> reducerType;
	private final ContainerType partitionedArgumentContainerType;

	public PartitionedRemoteServiceMethod(int partitionedArgumentIndex,
										  Method proxiedMethod,
										  String methodSignature, 
										  RemotingEngine remotingEngine,
										  Type targetReturnType) {
		this.partitionedArgumentIndex = partitionedArgumentIndex;
		this.methodSignature = methodSignature;
		this.remotingEngine = remotingEngine;
		this.targetReturnType = targetReturnType;
		AstrixPartitionedRouting partitionedRouting = getPartitionedRoutingAnnotation(proxiedMethod, partitionedArgumentIndex);
		this.reducerType = getReducer(partitionedRouting, proxiedMethod);
		this.partitionedArgumentContainerType = getPartititonedArgumentContainerType(proxiedMethod, partitionedRouting);
	}

	private ContainerType getPartititonedArgumentContainerType(Method proxiedMethod, AstrixPartitionedRouting partitionBy) {
		Class<?> partitionedArgumentType = proxiedMethod.getParameterTypes()[partitionedArgumentIndex];
		if (partitionedArgumentType.isArray()) {
			return new ArrayContainerType(partitionedArgumentType.getComponentType());
		}
		Class<? extends Collection<?>> collectionFactory = (Class<? extends Collection<?>>) partitionBy.collectionFactory();
		if (!proxiedMethod.getParameterTypes()[partitionedArgumentIndex].isAssignableFrom(collectionFactory)) {
			throw new IllegalArgumentException(String.format("Collection class supplied by @AstrixPartitionedRouting is not "
											 + "compatible with argument type, argumentType=%s classType=%s", 
											 proxiedMethod.getParameterTypes()[partitionedArgumentIndex].getName(),
											 collectionFactory));
		}
		return new CollectionContainerType(collectionFactory);
	}

	private Class<? extends AstrixRemoteResultReducer<?, ?>> getReducer(
			AstrixPartitionedRouting partitionBy, Method targetServiceMethod) {
		Class<? extends AstrixRemoteResultReducer<?,?>> reducerType = (Class<? extends AstrixRemoteResultReducer<?, ?>>) partitionBy.reducer();
		RemotingProxyUtil.validateRemoteResultReducer(targetServiceMethod, reducerType);
		return reducerType;
	}

	private static AstrixPartitionedRouting getPartitionedRoutingAnnotation(Method proxiedMethod, int partitionedByArgumentIndex) {
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
		ServiceInvocationPartitioner serviceInvocationPartitioner = new ServiceInvocationPartitioner();
		Observable<AstrixServiceInvocationResponse> serviceInvocationResponses = Observable.empty();
		for (RoutedServiceInvocationReqeust serivceInvocationReqeuest : serviceInvocationPartitioner.partitionInvocationRequest(args)) {
			serviceInvocationResponses = serviceInvocationResponses.mergeWith(serivceInvocationReqeuest.submitRequest(invocationRequest, args));
		}
		return reduce(serviceInvocationResponses);
	}

	private <T> Observable<T> reduce(Observable<AstrixServiceInvocationResponse> responses) {
		final AstrixRemoteResultReducer<T, T> reducer = newRemoteResultReducer();
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

	@SuppressWarnings("unchecked")
	private <T> AstrixRemoteResultReducer<T, T> newRemoteResultReducer() {
		return (AstrixRemoteResultReducer<T, T>) ReflectionUtil.newInstance(this.reducerType);
	}

	private ContainerBuilder newCollectionInstance() {
		return partitionedArgumentContainerType.newInstance();
	}

	private class RoutedServiceInvocationReqeust {
		private final ContainerBuilder routingKeys;
		private final RoutingKey targetPartitionRoutingKey;

		public RoutedServiceInvocationReqeust(ContainerBuilder keys, int targetPartition) {
			this.routingKeys = keys;
			this.targetPartitionRoutingKey = RoutingKey.create(targetPartition);
		}

		public void addKey(Object requestedKey) {
			this.routingKeys.add(requestedKey);
		}
		
		public Observable<AstrixServiceInvocationResponse> submitRequest(AstrixServiceInvocationRequest invocationRequest, Object[] unpartitionedArguments) {
			AstrixServiceInvocationRequest partitionedRequest = new AstrixServiceInvocationRequest();
			partitionedRequest.setAllHeaders(invocationRequest.getHeaders());
			Object[] requestForPartition = Arrays.copyOf(unpartitionedArguments, unpartitionedArguments.length);
			requestForPartition[partitionedArgumentIndex] = this.routingKeys.buildTarget();
			partitionedRequest.setArguments(remotingEngine.marshall(requestForPartition));
			return remotingEngine.processRoutedRequest(partitionedRequest, targetPartitionRoutingKey);
		}
	}
	
	private class ServiceInvocationPartitioner {
		private final RoutedServiceInvocationReqeust[] requests;
		
		public ServiceInvocationPartitioner() {
			this.requests = new RoutedServiceInvocationReqeust[remotingEngine.partitionCount()];
		}

		public Iterable<RoutedServiceInvocationReqeust> partitionInvocationRequest(Object[] args) {
			partitionedArgumentContainerType.iterateContainer(getContainerInstance(args), new Consumer() {
				@Override
				public void accept(Object element) {
					addRoutingKey(element);
				}
			});
			List<RoutedServiceInvocationReqeust> result = new LinkedList<>();
			for (RoutedServiceInvocationReqeust routedInvocationReqeust : requests) {
				if (routedInvocationReqeust != null) {
					result.add(routedInvocationReqeust);
				}
			}
			return result;
		}

		private Object getContainerInstance(Object[] args) {
			return args[partitionedArgumentIndex];
		}

		public void addRoutingKey(Object requestedKey) {
			int targetPartition = requestedKey.hashCode() % requests.length;
			RoutedServiceInvocationReqeust invocationRequestForPartition = this.requests[targetPartition];
			if (invocationRequestForPartition == null) {
				invocationRequestForPartition = new RoutedServiceInvocationReqeust(newCollectionInstance(), targetPartition);
				this.requests[targetPartition] = invocationRequestForPartition;
				
			}
			invocationRequestForPartition.addKey(requestedKey);
		}

	}
	
	private interface ContainerType {
		ContainerBuilder newInstance();
		<E> void iterateContainer(Object container, Consumer consumer);
	}
	
	interface Consumer {
		void accept(Object element);
	}
	
	private static class CollectionContainerType implements ContainerType {
		private final Class<? extends Collection<?>> collectionFactory;
		
		public CollectionContainerType(Class<? extends Collection<?>> collectionFactory) {
			this.collectionFactory = collectionFactory;
		}

		@Override
		public ContainerBuilder newInstance() {
			return new CollectionContainerBuilder((Collection<? super Object>) ReflectionUtil.newInstance(this.collectionFactory));
		}

		@Override
		public void iterateContainer(Object container, Consumer consumer) {
			for (Object element : (Collection<? extends Object>) container) {
				consumer.accept(element);
			}
		}
	}
	
	private static class ArrayContainerType implements ContainerType {
		
		private final Class<?> elementType;
		
		public ArrayContainerType(Class<?> elementType) {
			this.elementType = elementType;
		}

		@Override
		public ContainerBuilder newInstance() {
			return new ArrayContainerBuilder(elementType);
		}

		@Override
		public void iterateContainer(Object container, Consumer consumer) {
			for (int i = 0; i < Array.getLength(container); i++) {
				consumer.accept(Array.get(container, i));
			}
		}
	}
	
	private static class ArrayContainerBuilder extends ContainerBuilder {
		
		private final Class<?> elementType;
		private final List<Object> elements = new ArrayList<>();
		
		public ArrayContainerBuilder(Class<?> elementType) {
			this.elementType = elementType;
		}
		
		@Override
		void add(Object element) {
			elements.add(element);
		}
		
		@Override
		Object buildTarget() {
			Object array = Array.newInstance(elementType, elements.size());
			int nextIndex = 0;
			for (Object element : elements) {
				Array.set(array, nextIndex, element);
				nextIndex++;
			}
			return array;
		}
	}
	
	private static abstract class ContainerBuilder {
		
		abstract void add(Object element);
		
		abstract Object buildTarget();
	}
	
	private static class CollectionContainerBuilder extends ContainerBuilder {
		private Collection<? super Object> collection;

		public CollectionContainerBuilder(Collection<? super Object> collection) {
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
