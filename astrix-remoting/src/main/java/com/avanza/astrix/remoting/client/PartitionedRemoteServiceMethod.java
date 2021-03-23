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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

import com.avanza.astrix.core.AstrixPartitionedRouting;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.RemoteResultReducer;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.core.util.ReflectionUtil;
import rx.Observable;
import rx.functions.Func1;
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
	private final Class<? extends RemoteResultReducer<?>> reducerType;
	private final ContainerType partitionedArgumentContainerType;
	private final PartitionedRouter router;
	private final Method proxiedMethod;

	public PartitionedRemoteServiceMethod(int partitionedArgumentIndex,
										  Method proxiedMethod,
										  String methodSignature,
										  RemotingEngine remotingEngine,
										  Type targetReturnType,
										  Method targetServiceMethod) {
		this.partitionedArgumentIndex = partitionedArgumentIndex;
		this.proxiedMethod = proxiedMethod;
		this.methodSignature = methodSignature;
		this.remotingEngine = remotingEngine;
		this.targetReturnType = targetReturnType;
		AstrixPartitionedRouting partitionedRouting = getPartitionedRoutingAnnotation(proxiedMethod, partitionedArgumentIndex);
		this.reducerType = getReducer(partitionedRouting, targetServiceMethod);
		this.partitionedArgumentContainerType = getPartitionedArgumentContainerType(proxiedMethod, partitionedRouting);
		this.router = createRouter(partitionedRouting);
	}

	private PartitionedRouter createRouter(AstrixPartitionedRouting partitionedRouting) {
		Class<?> elementType = this.partitionedArgumentContainerType.getElementType();
		if (!partitionedRouting.routingMethod().isEmpty()) {
			Method routingMethod;
			try {
				routingMethod = elementType.getMethod(partitionedRouting.routingMethod());
				return PartitionedRouter.routingMethod(routingMethod);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new IllegalArgumentException("Failed to find routing method for partitioned routing:\n"
												 + "service: " + ReflectionUtil.fullMethodName(proxiedMethod) + "\n"
												 + "@AstrixPartitionedRouting.routingMethod: " + partitionedRouting.routingMethod(), e);
			}
		}
		return PartitionedRouter.identity();
	}

	private ContainerType getPartitionedArgumentContainerType(Method proxiedMethod, AstrixPartitionedRouting partitionBy) {
		Class<?> partitionedArgumentType = proxiedMethod.getParameterTypes()[partitionedArgumentIndex];
		if (partitionedArgumentType.isArray()) {
			return new ArrayContainerType(partitionedArgumentType.getComponentType());
		}
		@SuppressWarnings("unchecked")
		Class<? extends Collection<?>> collectionFactory = (Class<? extends Collection<?>>) partitionBy.collectionFactory();
		if (!proxiedMethod.getParameterTypes()[partitionedArgumentIndex].isAssignableFrom(collectionFactory)) {
			throw new IllegalArgumentException(String.format("Collection class supplied by @AstrixPartitionedRouting is not "
											 + "compatible with argument type, argumentType=%s classType=%s", 
											 proxiedMethod.getParameterTypes()[partitionedArgumentIndex].getName(),
											 collectionFactory));
		}
		Type rawType = proxiedMethod.getGenericParameterTypes()[partitionedArgumentIndex];
		if (!(rawType instanceof ParameterizedType)) {
			throw new IllegalArgumentException("Illegal service method: " + ReflectionUtil.fullMethodName(proxiedMethod) + ".\nWhen defining a routingMethod for @AstrixPartitionedRouting the target Collection type must not be a raw type. \nwas: " + rawType);
		}
		ParameterizedType partitionedArgumentTypeParameters = (ParameterizedType) rawType;
		return new CollectionContainerType(collectionFactory, (Class<?>)partitionedArgumentTypeParameters.getActualTypeArguments()[0]);
	}

	private Class<? extends RemoteResultReducer<?>> getReducer(AstrixPartitionedRouting partitionBy, Method targetServiceMethod) {
		@SuppressWarnings("unchecked")
		Class<? extends RemoteResultReducer<?>> reducerType = (Class<? extends RemoteResultReducer<?>>) partitionBy.reducer();
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
		List<RoutedServiceInvocationRequest> partitionInvocationRequest = serviceInvocationPartitioner.partitionInvocationRequest(invocationRequest, args);
		Observable<List<AstrixServiceInvocationResponse>> serviceInvocationResponses = remotingEngine.submitRoutedRequests(partitionInvocationRequest);
		return reduce(serviceInvocationResponses);
	}

	private <T> Observable<T> reduce(Observable<List<AstrixServiceInvocationResponse>> responses) {
		if (targetReturnType.equals(Void.TYPE) || targetReturnType.equals(Void.class)) {
			return responses.map(responseList -> {
				readResults(responseList);
				return null;
			});
		}
		final RemoteResultReducer<T> reducer = newRemoteResultReducer();
		return responses.map(responseList -> {
			List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>(responseList.size());
			for (AstrixServiceInvocationResponse response : responseList) {
				AstrixRemoteResult<T> result = remotingEngine.toRemoteResult(response, targetReturnType);
				unmarshalledResponses.add(result);
			}
			return reducer.reduce(unmarshalledResponses);
		});
		
	}

	private void readResults(List<AstrixServiceInvocationResponse> responseList) {
		responseList.forEach(res -> remotingEngine.toRemoteResult(res, targetReturnType).getResult());
	}

	@SuppressWarnings("unchecked")
	private <T> RemoteResultReducer<T> newRemoteResultReducer() {
		return (RemoteResultReducer<T>) ReflectionUtil.newInstance(this.reducerType);
	}

	private ContainerBuilder newCollectionInstance() {
		return partitionedArgumentContainerType.newInstance();
	}

	private class RoutedServiceInvocationRequestBuilder {
		private final ContainerBuilder routingKeys;
		private final RoutingKey targetPartitionRoutingKey;

		public RoutedServiceInvocationRequestBuilder(ContainerBuilder keys, int targetPartition) {
			this.routingKeys = keys;
			this.targetPartitionRoutingKey = RoutingKey.create(targetPartition);
		}

		public void addKey(Object requestedKey) {
			this.routingKeys.add(requestedKey);
		}
		
		private RoutedServiceInvocationRequest createInvocationRequest(
				AstrixServiceInvocationRequest invocationRequest,
				Object[] unpartitionedArguments) {
			AstrixServiceInvocationRequest partitionedRequest = new AstrixServiceInvocationRequest();
			partitionedRequest.setAllHeaders(invocationRequest.getHeaders());
			Object[] requestForPartition = Arrays.copyOf(unpartitionedArguments, unpartitionedArguments.length);
			requestForPartition[partitionedArgumentIndex] = this.routingKeys.buildTarget();
			partitionedRequest.setArguments(remotingEngine.marshall(requestForPartition));
			return new RoutedServiceInvocationRequest(partitionedRequest, targetPartitionRoutingKey);
		}
	}
	
	private class ServiceInvocationPartitioner {
		private final RoutedServiceInvocationRequestBuilder[] requests;
		
		public ServiceInvocationPartitioner() {
			this.requests = new RoutedServiceInvocationRequestBuilder[remotingEngine.partitionCount()];
		}

		public List<RoutedServiceInvocationRequest> partitionInvocationRequest(AstrixServiceInvocationRequest invocationRequest, Object[] args) {
			partitionedArgumentContainerType.iterateContainer(getContainerInstance(args), new Consumer<Object>() {
				@Override
				public void accept(Object element) {
					addElement(element);
				}
			});
			List<RoutedServiceInvocationRequest> result = new LinkedList<>();
			for (RoutedServiceInvocationRequestBuilder routedInvocationReqeustBuilder : requests) {
				if (routedInvocationReqeustBuilder != null) {
					result.add(routedInvocationReqeustBuilder.createInvocationRequest(invocationRequest, args));
				}
			}
			return result;
		}
		
		private Object getContainerInstance(Object[] args) {
			return args[partitionedArgumentIndex];
		}

		public void addElement(Object element) {
			RoutingKey routingKey = router.getRoutingKey(element);
			int targetPartition = routingKey.hashCode() % requests.length;
			RoutedServiceInvocationRequestBuilder invocationRequestBuilderForPartition = this.requests[targetPartition];
			if (invocationRequestBuilderForPartition == null) {
				invocationRequestBuilderForPartition = new RoutedServiceInvocationRequestBuilder(newCollectionInstance(), targetPartition);
				this.requests[targetPartition] = invocationRequestBuilderForPartition;
				
			}
			invocationRequestBuilderForPartition.addKey(element);
		}

	}
	
	private interface ContainerType {
		ContainerBuilder newInstance();
		void iterateContainer(Object container, Consumer<Object> consumer);
		Class<?> getElementType();
	}
	
	private static class CollectionContainerType implements ContainerType {
		private final Class<?> elementType;
		private final Class<? extends Collection<?>> collectionFactory;
		
		public CollectionContainerType(Class<? extends Collection<?>> collectionFactory, Class<?> elementType) {
			this.collectionFactory = Objects.requireNonNull(collectionFactory);
			this.elementType = Objects.requireNonNull(elementType);
		}

		@SuppressWarnings("unchecked")
		@Override
		public ContainerBuilder newInstance() {
			return new CollectionContainerBuilder((Collection<? super Object>) ReflectionUtil.newInstance(this.collectionFactory));
		}

		@Override
		public void iterateContainer(Object container, Consumer<Object> consumer) {
			for (Object element : (Collection<? extends Object>) container) {
				consumer.accept(element);
			}
		}
		
		@Override
		public Class<?> getElementType() {
			return elementType;
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
		public void iterateContainer(Object container, Consumer<Object> consumer) {
			for (int i = 0; i < Array.getLength(container); i++) {
				consumer.accept(Array.get(container, i));
			}
		}
		
		@Override
		public Class<?> getElementType() {
			return this.elementType;
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
