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
package com.avanza.astrix.remoting.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.Assert;

import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.DefaultTraceProvider;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.context.JavaSerializationSerializer;
import com.avanza.astrix.context.mbeans.MBeanExporter;
import com.avanza.astrix.context.metrics.Metrics;
import com.avanza.astrix.context.metrics.Timer;
import com.avanza.astrix.context.metrics.TimerSnaphot;
import com.avanza.astrix.context.metrics.TimerSpi;
import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.remoting.Router;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.RemotingProxy;
import com.avanza.astrix.remoting.client.RemotingTransport;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;

import rx.Observable;
import rx.Subscriber;

public class AstrixRemotingDriver {
	
	private AstrixObjectSerializer objectSerializer = new JavaSerializationSerializer(1);
	private final ConcurrentMap<MBeanKey, Object> mbeanByKey = new ConcurrentHashMap<>();
	private final Metrics metrics = new Metrics() {
		@Override
		public Timer createTimer() {
			return new Timer(new FakeTimer());
		}
	};
	private MBeanExporter exporter = new MBeanExporter() {
		@Override
		public void registerMBean(Object mbean, String folder, String name) {
			mbeanByKey.putIfAbsent(new MBeanKey(folder, name), mbean);
		}
	};
	private ReactiveTypeConverter reactiveTypeConverter = new ReactiveTypeConverterImpl(Collections.<ReactiveTypeHandlerPlugin<?>>emptyList());
	private DynamicBooleanProperty exportedServiceMetricsEnabled = new DynamicBooleanProperty(true);
	private final AstrixTraceProvider astrixTraceProvider;

	private AstrixServiceActivatorImpl[] partitions;
	
	public AstrixRemotingDriver() {
		this(1);
	}

	public AstrixRemotingDriver(AstrixTraceProvider astrixTraceProvider) {
		this(1, astrixTraceProvider);
	}

	public AstrixRemotingDriver(int partitionCount) {
		this(partitionCount, new DefaultTraceProvider());
	}

	public AstrixRemotingDriver(int partitionCount, AstrixTraceProvider astrixTraceProvider) {
		this.partitions = new AstrixServiceActivatorImpl[partitionCount];
		this.astrixTraceProvider = Objects.requireNonNull(astrixTraceProvider);
		IntStream.range(0, partitionCount).forEach(index -> partitions[index] = new AstrixServiceActivatorImpl(exportedServiceMetricsEnabled, metrics, exporter, astrixTraceProvider));
	}
	
	public <T> T hasExportedMbeanOfType(Class<T> expectedType, MBeanKey key) {
		
		Object mbean = this.mbeanByKey.get(key);
		Assert.assertNotNull("Expected an exported mbean with key: " + key, mbean);
		Assert.assertTrue("Mbean type, expected: " + expectedType + ", got: " + mbean.getClass(), expectedType.isAssignableFrom(mbean.getClass()));
		return expectedType.cast(mbean);
	}
	


	public <T> T createRemotingProxy(Class<T> proxyAndTargetApi) {
		return createRemotingProxy(proxyAndTargetApi, proxyAndTargetApi);
	}

	public <T> T createRemotingProxy(Class<T> proxyApi, Class<?> targetApi) {
		return RemotingProxy.create(proxyApi, targetApi, directTransport(), objectSerializer, new NoRoutingStrategy(), reactiveTypeConverter, astrixTraceProvider);
	}
	
	public <T> T createRemotingProxy(Class<T> proxyApi, Class<?> targetApi,	AstrixObjectSerializer objectSerializerOverride) {
		return RemotingProxy.create(proxyApi, targetApi, directTransport(), objectSerializerOverride, new NoRoutingStrategy(), reactiveTypeConverter, astrixTraceProvider);
	}
	
	public <T> T createRemotingProxy(Class<T> proxyApi, Class<?> targetApi,	RoutingStrategy routingStrategyOverride) {
		return RemotingProxy.create(proxyApi, targetApi, directTransport(), objectSerializer, routingStrategyOverride, reactiveTypeConverter, astrixTraceProvider);
	}
	
	/**
	 * Registers a service in the first partition
	 */
	public <T> void registerServer(Class<T> publishedApi, Object provider) {
		this.partitions[0].register(provider, objectSerializer, publishedApi);
	}
	
	public <T> void registerServerPartition(int paritionIndex, Class<T> publishedApi, T provider) {
		this.partitions[paritionIndex].register(provider, objectSerializer, publishedApi);
	}
	
	
	private RemotingTransport directTransport() {
		return RemotingTransport.create(new PartitionedDirectTransport(Arrays.asList(this.partitions)));
	}
	
	
	private static class PartitionedDirectTransport implements RemotingTransportSpi {

		private List<AstrixServiceActivatorImpl> partitions;
		
		public PartitionedDirectTransport(List<AstrixServiceActivatorImpl> partitions) {
			this.partitions = partitions;
		}

		@Override
		public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey){
			final AstrixServiceInvocationResponse response = getActivator(routingKey).invokeService(request);
			return Observable.unsafeCreate(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(Subscriber<? super AstrixServiceInvocationResponse> t1) {
					t1.onNext(response);
					t1.onCompleted();
				}
			});
		}

		private AstrixServiceActivatorImpl getActivator(RoutingKey routingKey) {
			return partitions.get(routingKey.hashCode() % partitions.size());
		}

		@Override
		public Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(AstrixServiceInvocationRequest request) {
			final List<AstrixServiceInvocationResponse> responses = new ArrayList<>();
			for (AstrixServiceActivatorImpl partition : partitions) {
				responses.add(partition.invokeService(request));
			}
			return Observable.unsafeCreate(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(Subscriber<? super AstrixServiceInvocationResponse> t1) {
					for (AstrixServiceInvocationResponse r : responses) {
						t1.onNext(r);
					}
					t1.onCompleted();
				}
			}).toList();
			
		}

		@Override
		public int partitionCount() {
			return this.partitions.size();
		}

		@Override
		public Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(Collection<RoutedServiceInvocationRequest> requests) {
			Observable<AstrixServiceInvocationResponse> result = Observable.empty();
			for (RoutedServiceInvocationRequest request : requests) {
				result = result.mergeWith(submitRoutedRequest(request.getRequest(), request.getRoutingkey()));
			}
			return result.toList();
		}
		
	}
	
	private static class NoRoutingStrategy implements RoutingStrategy {
		@Override
		public Router create(Method serviceMethod) {
			if (serviceMethod.isAnnotationPresent(AstrixBroadcast.class)) {
				return new Router() {
					@Override
					public RoutingKey getRoutingKey(Object[] args) throws Exception {
						return null; // Broadcast
					}
				};
			}
			return new Router() {
				@Override
				public RoutingKey getRoutingKey(Object[] args) throws Exception {
					return RoutingKey.create(1); // Constant routing
				}
				
			};
		}
	}
	
	
	private static class FakeTimer implements TimerSpi {
		private AtomicInteger invocationCount = new AtomicInteger(0);
		@Override
		public <T> CheckedCommand<T> timeExecution(CheckedCommand<T> execution) {
			invocationCount.incrementAndGet();
			return execution;
		}
		@Override
		public <T> Supplier<Observable<T>> timeObservable(Supplier<Observable<T>> observableFactory) {
			return () -> {
				invocationCount.incrementAndGet();
				return observableFactory.get();
			};
		}

		@Override
		public TimerSnaphot getSnapshot() {
			return new TimerSnaphot.Builder().count(invocationCount.get()).build();
		}
		
	}

	public void setExportedServiceMetricsEnabled(boolean enabled) {
		this.exportedServiceMetricsEnabled.set(enabled);
	}

}
