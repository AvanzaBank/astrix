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

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;

import com.avanza.astrix.context.JavaSerializationSerializer;
import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.AstrixPartitionedRouting;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.RemoteResultReducer;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceInvocationException;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.DefaultAstrixRoutingStrategy;
import com.avanza.astrix.remoting.client.IncompatibleRemoteResultReducerException;
import com.avanza.astrix.remoting.client.MissingServiceException;
import com.avanza.astrix.remoting.client.RemotingProxy;
import com.avanza.astrix.remoting.client.RemotingTransport;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;
import com.avanza.astrix.remoting.client.Router;
import com.avanza.astrix.remoting.client.RoutingKey;
import com.avanza.astrix.remoting.client.RoutingStrategy;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixRemotingTest {

	AstrixObjectSerializer objectSerializer = new JavaSerializationSerializer(1);
	AstrixServiceActivator partition1 = new AstrixServiceActivator();
	
	private static class NoRoutingStrategy implements RoutingStrategy {
		@Override
		public Router create(Method serviceMethod) {
			if (serviceMethod.isAnnotationPresent(AstrixBroadcast.class)) {
				return new Router() {
					@Override
					public RoutingKey getRoutingKey(Object... args) throws Exception {
						return null; // Broadcast
					}
				};
			}
			return new Router() {
				@Override
				public RoutingKey getRoutingKey(Object... args) throws Exception {
					return RoutingKey.create(1); // Constant routing
				}
				
			};
		}
	}
	
	@Test
	public void invokesAServiceThroughTransportAndServiceActivator() throws Exception {
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
			@Override
			public String hello(HelloRequest message, String greeting) {
				return "overload-" + message.getMesssage();
			}
		};
		partition1.register(impl, objectSerializer, TestService.class);
		
		TestService testService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		HelloRequest request = new HelloRequest("kalle");
		HelloResponse reply = testService.hello(request);
		assertEquals("reply-kalle", reply.getGreeting());
	}
	
	private static RemotingTransport directTransport(AstrixServiceActivator... partitions) {
		return RemotingTransport.create(new PartitionedDirectTransport(Arrays.asList(partitions)));
	}
	
	@Test
	public void supportsServiceMethodsWithMultipleArguments() throws Exception {
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}

			@Override
			public String hello(HelloRequest message, String greeting) {
				return greeting + message.getMesssage();
			}
			
			
		};
		
		partition1.register(impl, objectSerializer, TestService.class);
		
		TestService testService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		HelloRequest request = new HelloRequest("kalle");
		String reply = testService.hello(request, "replyTo-");
		assertEquals("replyTo-kalle", reply);
	}
	
	
	@Test
	public void usesRoutingAnnotationOnServiceMethodToRouteServiceInvocations() throws Exception {
		AnnotatedArgumentTestService serviceProvider = new AnnotatedArgumentTestService() {
			@Override
			public String hello(String message, String greeting) {
				return greeting + message;
			}
			
			
		};
		partition1.register(serviceProvider, objectSerializer, AnnotatedArgumentTestService.class);
		
		AnnotatedArgumentTestService serviceProxy = RemotingProxy.create(AnnotatedArgumentTestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		String reply = serviceProxy.hello("kalle", "hello-");
		assertEquals("hello-kalle", reply);
	}
	
	
	@Test
	public void routedRequest_throwsExceptionOfNonServiceInvocationExceptionType() throws Exception {
		try {
			TestService impl = new TestService() {
				@Override
				public HelloResponse hello(HelloRequest message) {
					throw new IllegalArgumentException("Remote service error message");
				}
				@Override
				public String hello(HelloRequest message, String greeting) {
					return "overload-" + message.getMesssage();
				}
			};
			partition1.register(impl, objectSerializer, TestService.class);

			TestService testService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
			testService.hello(new HelloRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (RemoteServiceInvocationException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Remote service error message]"));
		}
	}
	
	@Test
	public void routedRequest_throwsExceptionOfServiceInvocationType() throws Exception {
		try {
			TestService impl = new TestService() {
				@Override
				public HelloResponse hello(HelloRequest message) {
					throw new MyCustomServiceException();
				}
				@Override
				public String hello(HelloRequest message, String greeting) {
					return "overload-" + message.getMesssage();
				}
			};
			partition1.register(impl, objectSerializer, TestService.class);

			TestService testService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
			testService.hello(new HelloRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (MyCustomServiceException e) {
			// Expected
		} catch (Exception e) {
			e.printStackTrace();
			fail("Excpected exception of type MyCustomServiceException, but was: " + e);
		}
	}
	

	@Test
	public void broadcastRequest() throws Exception {
		AstrixServiceActivator partition2 = new AstrixServiceActivator();
		PingService impl = new PingService() {
			@Override
			public List<String> ping(String msg) {
				return Arrays.asList(msg);
			}
		};
		partition1.register(impl, objectSerializer, PingService.class);
		partition2.register(impl, objectSerializer, PingService.class);

		PingService broadcastService = RemotingProxy.create(PingService.class, directTransport(partition1, partition2), objectSerializer, new NoRoutingStrategy());
		List<String> replys = broadcastService.ping("foo");
		assertEquals(2, replys.size());
		assertEquals("foo", replys.get(0));
		assertEquals("foo", replys.get(1));
	}
	
	@Test
	public void partitionedRequest() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		AstrixServiceActivator oddPartition = new AstrixServiceActivator();
		CalculatorListService eventPartitionCalculator = new CalculatorListService() {
			@Override
			public Integer squareSum(Collection<Integer> nums) {
				int squareSum = 0;
				for (int num : nums) {
					if (num % 2 != 0) {
						throw new AssertionError("Even Partition should only receive even numbers, but received: " + num);
					}
					squareSum += num * num;
				}
				return squareSum;
			}
		};
		CalculatorListService oddPartitionCalculator = new CalculatorListService() {
			@Override
			public Integer squareSum(Collection<Integer> nums) {
				int squareSum = 0;
				for (int num : nums) {
					if (num % 2 != 1) {
						throw new AssertionError("Odd Partition hould only receive odd numbers, but received: " + num);
					}
					squareSum += num * num;
				}
				return squareSum;
			}
		};
		
		evenPartition.register(eventPartitionCalculator, objectSerializer, CalculatorListService.class);
		oddPartition.register(oddPartitionCalculator, objectSerializer, CalculatorListService.class);

		CalculatorListService calculatorService = RemotingProxy.create(CalculatorListService.class, directTransport(evenPartition, oddPartition), objectSerializer, new NoRoutingStrategy());
		int squareSum = calculatorService.squareSum(Arrays.asList(1, 2, 3, 4, 5));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test
	public void partitionedRequest_GenericArrayArgument() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		AstrixServiceActivator oddPartition = new AstrixServiceActivator();
		PartitionedPingService eventPartitionPing = new PartitionedPingServiceImpl();
		PartitionedPingService oddPartitionPing = new PartitionedPingServiceImpl();
		
		evenPartition.register(eventPartitionPing, objectSerializer, PartitionedPingService.class);
		oddPartition.register(oddPartitionPing, objectSerializer, PartitionedPingService.class);

		PartitionedPingService partitionedPing = RemotingProxy.create(PartitionedPingService.class, directTransport(evenPartition, oddPartition), objectSerializer, new NoRoutingStrategy());
		assertThat(partitionedPing.ping(new double[]{1d, 2d, 3d}), containsInAnyOrder(1d, 2d, 3d));
		assertThat(partitionedPing.ping(new int[]{1, 2, 3}), containsInAnyOrder(1, 2, 3));
		assertThat(partitionedPing.ping(new float[]{1f, 2f, 3f}), containsInAnyOrder(1f, 2f, 3f));
		assertThat(partitionedPing.ping(new short[]{1, 2, 3}), containsInAnyOrder(new Short[]{1, 2, 3}));
		assertThat(partitionedPing.ping(new long[]{1L, 2L, 3L}), containsInAnyOrder(1L, 2L, 3L));
		assertThat(partitionedPing.ping(new String[]{"1", "2", "3"}), containsInAnyOrder("1", "2", "3"));
	}
	
	@Test
	public void partitionedRequest_voidReturnType() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		AstrixServiceActivator oddPartition = new AstrixServiceActivator();
		PartitionedPingService eventPartitionPing = new PartitionedPingServiceImpl();
		PartitionedPingService oddPartitionPing = new PartitionedPingServiceImpl();
		
		evenPartition.register(eventPartitionPing, objectSerializer, PartitionedPingService.class);
		oddPartition.register(oddPartitionPing, objectSerializer, PartitionedPingService.class);

		PartitionedPingService partitionedPing = RemotingProxy.create(PartitionedPingService.class, directTransport(evenPartition, oddPartition), objectSerializer, new NoRoutingStrategy());
		partitionedPing.pingVoid(new String[]{"1", "2", "3"});
	}
	
	@Test
	public void partitionedRequest_routingOnPropertyOnTargetObject() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		AstrixServiceActivator oddPartition = new AstrixServiceActivator();
		CalculatorArrayPojoService eventPartitionCalculator = new CalculatorArrayPojoService() {
			@Override
			public Integer squareSum(NumPojo... nums) {
				int squareSum = 0;
				for (NumPojo numPojo : nums) {
					int num = numPojo.getNum();
					if (num % 2 != 0) {
						throw new AssertionError("Even Partition should only receive even numbers, but received: " + num);
					}
					squareSum += num * num;
				}
				return squareSum;
			}

		};
		CalculatorArrayPojoService oddPartitionCalculator = new CalculatorArrayPojoService() {
			@Override
			public Integer squareSum(NumPojo... nums) {
				int squareSum = 0;
				for (NumPojo numPojo : nums) {
					int num = numPojo.getNum();
					if (num % 2 != 1) {
						throw new AssertionError("Odd Partition hould only receive odd numbers, but received: " + num);
					}
					squareSum += num * num;
				}
				return squareSum;
			}
		};
		
		evenPartition.register(eventPartitionCalculator, objectSerializer, CalculatorArrayPojoService.class);
		oddPartition.register(oddPartitionCalculator, objectSerializer, CalculatorArrayPojoService.class);

		CalculatorArrayPojoService calculatorService = RemotingProxy.create(CalculatorArrayPojoService.class, directTransport(evenPartition, oddPartition), objectSerializer, new NoRoutingStrategy());
		int squareSum = calculatorService.squareSum(new NumPojo(1), new NumPojo(2), new NumPojo(3), new NumPojo(4), new NumPojo(5));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test
	public void partitionedRequest_routingOnPropertyOnTargetObject_CollectionArgument() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		AstrixServiceActivator oddPartition = new AstrixServiceActivator();
		CalculatorListPojoService eventPartitionCalculator = new CalculatorListPojoServiceImpl();
		CalculatorListPojoService oddPartitionCalculator = new CalculatorListPojoServiceImpl();
		
		evenPartition.register(eventPartitionCalculator, objectSerializer, CalculatorListPojoService.class);
		oddPartition.register(oddPartitionCalculator, objectSerializer, CalculatorListPojoService.class);

		CalculatorListPojoService calculatorService = RemotingProxy.create(CalculatorListPojoService.class, directTransport(evenPartition, oddPartition), objectSerializer, new NoRoutingStrategy());
		int squareSum = calculatorService.squareSum(Arrays.asList(new NumPojo(1), new NumPojo(2), new NumPojo(3), new NumPojo(4), new NumPojo(5)));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedRequest_routingOnProperty_throwsExceptionForRawTypes() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		RemotingProxy.create(ServiceWithRawListRoutingArgument.class, directTransport(evenPartition), objectSerializer, new DefaultAstrixRoutingStrategy());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedRequest_routingOnProperty_throwsExceptionForMissingMethods() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		RemotingProxy.create(ServiceWithListMissingRoutingPropertyMethod.class, directTransport(evenPartition), objectSerializer, new DefaultAstrixRoutingStrategy());
	}
	
	public interface ServiceWithRawListRoutingArgument {
		List<Integer> foo(@AstrixPartitionedRouting(routingMethod = "getRoutingKey") List list);
	}
	
	public interface ServiceWithListMissingRoutingPropertyMethod {
		List<Integer> foo(@AstrixPartitionedRouting(routingMethod = "aMissingMethod") List<NumPojo> list);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedService_IncompatibleCollectionType_throwsException() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		RemotingProxy.create(InvalidCollectionTypePartitionedService.class, directTransport(evenPartition), objectSerializer, new NoRoutingStrategy());
	}
	
	@Test(expected = IncompatibleRemoteResultReducerException.class)
	public void partitionedService_IncompatibleReducer_throwsException() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		RemotingProxy.create(InvalidReducerPartitionedService.class, directTransport(evenPartition), objectSerializer, new NoRoutingStrategy());
	}
	
	@Test
	public void partitionedService_NonListCollection() throws Exception {
		AstrixServiceActivator evenPartition = new AstrixServiceActivator();
		PartitionedServiceUsingSet sevenPartitionService = new PartitionedServiceUsingSet() {
			@Override
			public Set<Integer> ping(Set<Integer> nums) {
				return nums;
			}
		};
		
		evenPartition.register(sevenPartitionService, objectSerializer, PartitionedServiceUsingSet.class);

		PartitionedServiceUsingSet calculatorService = RemotingProxy.create(PartitionedServiceUsingSet.class, 
				directTransport(evenPartition), objectSerializer, new NoRoutingStrategy());
		assertEquals(setOf(1,2,3), calculatorService.ping(setOf(1,2,3)));
	}
	
	private static <T> Set<T> setOf(@SuppressWarnings("unchecked") T... values) {
		return new HashSet<T>(Arrays.asList(values));
	}

	@Test
	public void broadcastRequest_throwsException() throws Exception {
		try {
			BroadcastService impl = new BroadcastService() {
				@Override
				public String broadcast(BroadcastRequest request) {
					throw new IllegalArgumentException("Broadcast error message");
				}
			};
			partition1.register(impl, objectSerializer, BroadcastService.class);

			BroadcastService broadcastService = RemotingProxy.create(BroadcastService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
			broadcastService.broadcast(new BroadcastRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (RemoteServiceInvocationException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Broadcast error message]"));
		}
	}
	
	@Test
	public void publishMultipleApis() throws Exception {
		class MultiProvider implements BroadcastService, TestService {

			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("hello " + message.getMesssage());
			}

			@Override
			public String hello(HelloRequest message, String greeting) {
				return hello(message).getGreeting();
			}

			@Override
			@AstrixBroadcast(reducer = BroadcastReducer.class)
			public String broadcast(BroadcastRequest request) {
				return "broadcast-" + request.getMesssage();
			}
			
		}
		MultiProvider provider = new MultiProvider();
		partition1.register(provider, objectSerializer, BroadcastService.class);
		partition1.register(provider, objectSerializer, TestService.class);

		BroadcastService broadcastService = RemotingProxy.create(BroadcastService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		TestService testService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		
		assertEquals(provider.hello(new HelloRequest("kalle")), testService.hello(new HelloRequest("kalle")));
		assertEquals(provider.broadcast(new BroadcastRequest("kalle")), broadcastService.broadcast(new BroadcastRequest("kalle")));
	}
	
	@Test(expected = MissingServiceException.class)
	public void request_NoCorrespondingService_throwsException() throws Exception {
		TestService missingRemoteService = RemotingProxy.create(TestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		missingRemoteService.hello(new HelloRequest("foo"));
	}
	
	@Test
	public void useObservableVersionOfAService() throws Exception {
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
			@Override
			public String hello(HelloRequest message, String greeting) {
				return "overload-" + message.getMesssage();
			}
		};
		partition1.register(impl, objectSerializer, TestService.class);
		
		ObservableTestService service = RemotingProxy.create(ObservableTestService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		Observable<HelloResponse> message = service.hello(new HelloRequest("kalle"));
		assertEquals("reply-kalle", message.toBlocking().first().getGreeting());
	}
	
	@Test
	public void useAsyncVersionOfAService() throws Exception {
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
			@Override
			public String hello(HelloRequest message, String greeting) {
				return "overload-" + message.getMesssage();
			}
		};
		partition1.register(impl, objectSerializer, TestService.class);
		
		TestServiceAsync service = RemotingProxy.create(TestServiceAsync.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		Future<HelloResponse> response = service.hello(new HelloRequest("kalle"));
		assertEquals("reply-kalle", response.get().getGreeting());
	}
	
	@Test(expected = RuntimeException.class)
	public void ioExceptionThrownDuringDeserializationAreProppagatedAsRuntimeExceptions() throws Exception {
		AstrixObjectSerializer corruptDeserializer = new AstrixObjectSerializer.NoVersioningSupport() {
			@Override
			public <T> T deserialize(Object element, Type target, int version) {
				if (target.equals(HelloResponse.class)) {
					// simulate failure in deserializing service invocation response
					throw new IllegalArgumentException("phew.. I/O, huh?");
				}
				return super.deserialize(element, target, version);
			}
			
		};
		partition1 = new AstrixServiceActivator();
		
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
			@Override
			public String hello(HelloRequest message, String greeting) {
				return "overload-" + message.getMesssage();
			}
		};
		partition1.register(impl, objectSerializer, TestService.class);
		
		
		ObservableTestService service = RemotingProxy.create(ObservableTestService.class, directTransport(partition1), corruptDeserializer, new NoRoutingStrategy());
		Observable<HelloResponse> message = service.hello(new HelloRequest("kalle"));
		message.toBlocking().first();
	}
	
	@Test
	public void supportServicesThatAcceptAndReturnGenericTypes() throws Exception {
		GenericReturnTypeService impl = new GenericReturnTypeService() {
			@Override
			public List<HelloResponse> hello(String routing, List<HelloRequest> greetings) {
				List<HelloResponse> responses = new ArrayList<>();
				for (HelloRequest request : greetings) {
					responses.add(new HelloResponse("reply-" + request.getMesssage()));
				}
				return responses;
			}
		};
		partition1.register(impl, objectSerializer, GenericReturnTypeService.class);
		
		GenericReturnTypeService testService = RemotingProxy.create(GenericReturnTypeService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		HelloRequest request = new HelloRequest("kalle");
		List<HelloResponse> reply = testService.hello("foo-routing", Arrays.<HelloRequest>asList(request));
		assertEquals(1, reply.size());
		assertEquals("reply-kalle", reply.get(0).getGreeting());
	}
	
	@Test
	public void supportServicesThatAcceptAndReturnGenericOnBroadcast() throws Exception {
		BroadcastingGenericReturnTypeService impl = new BroadcastingGenericReturnTypeService() {
			@Override
			public List<HelloResponse> hello(List<HelloRequest> greetings) {
				List<HelloResponse> responses = new ArrayList<>();
				for (HelloRequest request : greetings) {
					responses.add(new HelloResponse("reply-" + request.getMesssage()));
				}
				return responses;
			}
		};
		partition1.register(impl, objectSerializer, BroadcastingGenericReturnTypeService.class);
		
		BroadcastingGenericReturnTypeService testService = RemotingProxy.create(BroadcastingGenericReturnTypeService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		HelloRequest request = new HelloRequest("kalle");
		List<HelloResponse> reply = testService.hello(Arrays.<HelloRequest>asList(request));
		assertEquals(1, reply.size());
		assertEquals("reply-kalle", reply.get(0).getGreeting());
	}
	
	@Test
	public void supportServicesWithNoArgument() throws Exception {
		NoArgumentService impl = new NoArgumentService() {
			@Override
			public List<String> hello() {
				return Arrays.asList("response");
			}
		};
		partition1.register(impl, objectSerializer, NoArgumentService.class);
		
		NoArgumentService testService = RemotingProxy.create(NoArgumentService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		List<String> reply = testService.hello();
		assertEquals(1, reply.size());
		assertEquals("response", reply.get(0));
	}
	
	@Test
	public void supportsServicesThatWithVoidReturnType() throws Exception {
		final BlockingQueue<String> receivedRequest = new LinkedBlockingQueue<>();
		VoidService impl = new VoidService() {
			@Override
			public void hello(String message) {
				receivedRequest.add(message);
			}
		};
		partition1.register(impl, objectSerializer, VoidService.class);
		
		VoidService testService = RemotingProxy.create(VoidService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(1, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test
	public void supportsBroadcastedServicesWithVoidReturnType() throws Exception {
		final BlockingQueue<String> receivedRequest = new LinkedBlockingQueue<>();
		BroadcastVoidService impl = new BroadcastVoidService() {
			@Override
			public void hello(String message) {
				receivedRequest.add(message);
			}
		};
		partition1.register(impl, objectSerializer, BroadcastVoidService.class);
		
		BroadcastVoidService testService = RemotingProxy.create(BroadcastVoidService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(0, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWhenRegisteringProviderForNonImplementedInterface() throws Exception {
		partition1.register(new Object(), objectSerializer, TestService.class);
	}
	
	@Test
	public void remotingProxiesDoesNotDelegateMethodCallsForMethodsDefinedIn_java_lang_Object() throws Exception {
		partition1.register(new VoidService() {
			@Override
			public void hello(String message) {
			}
			@Override
			public String toString() {
				return "RemotingServiceToString";
			}
		}, objectSerializer, VoidService.class);
		
		VoidService testService = RemotingProxy.create(VoidService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		assertEquals("RemotingProxy[" + VoidService.class.getName() + "]", testService.toString());
	}
	
	@Test
	public void asyncBroadcastedService() throws Exception {
		partition1.register(new BroadcastingGenericReturnTypeService() {
			@Override
			public List<HelloResponse> hello(List<HelloRequest> greeting) {
				return Arrays.asList(new HelloResponse(greeting.get(0).getMesssage()));
			}
			
		}, objectSerializer, BroadcastingGenericReturnTypeService.class);
		
		BroadcastingGenericReturnTypeServiceAsync service = RemotingProxy.create(BroadcastingGenericReturnTypeServiceAsync.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
		Future<List<HelloResponse>> resultFuture = service.hello(Arrays.asList(new HelloRequest("foo")));
		List<HelloResponse> result = resultFuture.get();
		assertEquals(1, result.size());
	}
	
	@Test(expected = IncompatibleRemoteResultReducerException.class)
	public void throwsExceptionOnProxyCreationIfRemoteResultReducerDoesNotHaveAMethodSignatureCompatibleWithServiceMethodSignature() throws Exception {
		RemotingProxy.create(BroadcastServiceWithIllegalReducer.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
	}
	
//	@Test(expected = IncompatibleRemoteResultReducerException.class)
//	public void throwsExceptionOnProxyCreationIfRemoteResultReducerDoesNotHaveAMethodSignatureCompatibleWithServiceMethodSignature_2() throws Exception {
//		RemotingProxy.create(IllegalReducerHelloService.class, directTransport(partition1), objectSerializer, new NoRoutingStrategy());
//	}
	
	@Test(expected = IllegalStateException.class)
	public void throwsIllegalStateExceptionIfRoutingStrategyReturnsNull() throws Exception {
		partition1.register(new VoidService() {
			@Override
			public void hello(String message) {
			}
		}, objectSerializer, VoidService.class);
		
		VoidService voidService = RemotingProxy.create(VoidService.class, directTransport(partition1), objectSerializer, new RoutingStrategy() {
			@Override
			public Router create(Method serviceMethod) {
				return new Router() {
					@Override
					public RoutingKey getRoutingKey(Object... args) throws Exception {
						return null;
					}
				};
			}
		});
		voidService.hello("foo");
	}
	
	@SuppressWarnings("serial")
	public static class HelloRequest implements Serializable {
		private String messsage;
		
		public HelloRequest(String messsage) {
			this.messsage = messsage;
		}
		
		public HelloRequest() {
		}
		
		public String getMesssage() {
			return messsage;
		}

		public void setMesssage(String messsage) {
			this.messsage = messsage;
		}
	}
	
	@SuppressWarnings("serial")
	public static class BroadcastRequest implements Serializable {
		private String messsage;
		
		public BroadcastRequest(String messsage) {
			this.messsage = messsage;
		}
		
		public BroadcastRequest() {
		}
		
		public String getMesssage() {
			return messsage;
		}

		public void setMesssage(String messsage) {
			this.messsage = messsage;
		}
	}
	
	@SuppressWarnings("serial")
	public static class HelloResponse implements Serializable {
		private String greeting;
		
		public HelloResponse() {
		}
		
		public HelloResponse(String greeting) {
			this.greeting = greeting;
		}

		public String getGreeting() {
			return greeting;
		}
		
		public void setGreeting(String greeting) {
			this.greeting = greeting;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public String toString() {
			return this.greeting;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return toString().equals(obj.toString());
		}
		
		
	}
	
	interface NoArgumentService {
		@AstrixBroadcast
		List<String> hello();
	}
	
	interface GenericReturnTypeService {
		List<HelloResponse> hello(String routingKey, List<HelloRequest> greeting);
	}
	
	interface BroadcastingGenericReturnTypeService {
		@AstrixBroadcast
		List<HelloResponse> hello(List<HelloRequest> greeting);
	}
	
	interface PingService {
		@AstrixBroadcast
		List<String> ping(String msg);
	}
	
	interface CalculatorListService {
		Integer squareSum(@AstrixPartitionedRouting(reducer = SummingReducer.class) Collection<Integer> nums);
	}
	
	interface CalculatorArrayPojoService {
		Integer squareSum(@AstrixPartitionedRouting(routingMethod="getNum", reducer = SummingReducer.class) NumPojo... nums);
	}
	
	interface CalculatorListPojoService {
		Integer squareSum(@AstrixPartitionedRouting(routingMethod="getNum", reducer = SummingReducer.class) List<NumPojo> nums);
	}
	
	static class CalculatorListPojoServiceImpl implements CalculatorListPojoService {

		@Override
		public Integer squareSum(List<NumPojo> nums) {
			int result = 0;
			for (NumPojo n : nums) {
				result += n.getNum() * n.getNum();
			}
			return result;
		}
		
	}
	
	interface PartitionedPingService {
		List<Short> ping(@AstrixPartitionedRouting short... nums);
		List<Integer> ping(@AstrixPartitionedRouting int... nums);
		List<Long> ping(@AstrixPartitionedRouting long... nums);
		List<Double> ping(@AstrixPartitionedRouting double... nums);
		List<Float> ping(@AstrixPartitionedRouting float... nums);
		List<Boolean> ping(@AstrixPartitionedRouting boolean... nums);
		List<String> ping(@AstrixPartitionedRouting String... nums);
		void pingVoid(@AstrixPartitionedRouting String... nums);
	}
	
	
	public static class NumPojo implements Serializable {
		private static final long serialVersionUID = 1L;
		private int num;
		public NumPojo(int num) {
			this.num = num;
		}
		public int getNum() {
			return num;
		}

		@Override
		public int hashCode() {
			// Routes all arguments to same partition
			return 1;
		}
	}
	
	interface InvalidCollectionTypePartitionedService {
		Integer squareSum(@AstrixPartitionedRouting(reducer = SummingReducer.class) Set<Integer> nums);
	}
	
	interface InvalidReducerPartitionedService {
		int squareSum(@AstrixPartitionedRouting(collectionFactory = HashSet.class) Set<Integer> nums);
	}
	
	interface PartitionedServiceUsingSet {
		Set<Integer> ping(@AstrixPartitionedRouting(reducer = SetReducer.class, collectionFactory = HashSet.class) Set<Integer> nums);
	}
	
	public static class SetReducer<T> implements RemoteResultReducer<Set<T>> {
		@Override
		public Set<T> reduce(List<AstrixRemoteResult<Set<T>>> results) {
			Set<T> result = new HashSet<>();
			for (AstrixRemoteResult<Set<T>> remoteResult : results) {
				result.addAll(remoteResult.getResult());
			}
			return result;
		}

	}
	
	public static class PartitionedPingServiceImpl implements PartitionedPingService {
		
		@Override
		public List<Short> ping(short... nums) {
			List<Short> result = new ArrayList<>();
			for (short s : nums) {
				result.add(s);
			}
			return result;
		}
		@Override
		public List<Integer> ping(int... nums) {
			List<Integer> result = new ArrayList<>();
			for (int s : nums) {
				result.add(s);
			}
			return result;
		}
		@Override
		public List<Long> ping(long... nums) {
			List<Long> result = new ArrayList<>();
			for (long s : nums) {
				result.add(s);
			}
			return result;
		}

		@Override
		public List<Double> ping(double... nums) {
			List<Double> result = new ArrayList<>();
			for (double s : nums) {
				result.add(s);
			}
			return result;
		}

		@Override
		public List<Float> ping(float... nums) {
			List<Float> result = new ArrayList<>();
			for (float s : nums) {
				result.add(s);
			}
			return result;
		}

		@Override
		public List<Boolean> ping(boolean... nums) {
			List<Boolean> result = new ArrayList<>();
			for (boolean s : nums) {
				result.add(s);
			}
			return result;
		}

		@Override
		public List<String> ping(String... nums) {
			List<String> result = new ArrayList<>();
			for (String s : nums) {
				result.add(s);
			}
			return result;
		}
		@Override
		public void pingVoid(String... nums) {
			
		}
	}
	
	public static class SummingReducer implements RemoteResultReducer<Integer> {
		@Override
		public Integer reduce(List<AstrixRemoteResult<Integer>> results) {
			int sum = 0;
			for (AstrixRemoteResult<Integer> result : results) {
				sum += result.getResult();
			}
			return sum;
		}
		
	}
	
	interface BroadcastingGenericReturnTypeServiceAsync {
		@AstrixBroadcast
		Future<List<HelloResponse>> hello(List<HelloRequest> greeting);
	}

	
	interface TestService {
		HelloResponse hello(HelloRequest message);
		String hello(HelloRequest message, String greeting);
	}
	
	interface VoidService {
		void hello(String message);
	}
	
	interface BroadcastVoidService {
		@AstrixBroadcast
		void hello(String message);
	}
	
	interface AnnotatedArgumentTestService {
		String hello(String message, String greeting);
	}
	
	interface ObservableTestService {
		Observable<HelloResponse> hello(HelloRequest message);
	}
	
	interface TestServiceAsync {
		Future<HelloResponse> hello(HelloRequest message);
	}
	
	interface BroadcastService {
		@AstrixBroadcast(reducer = GenericReducer.class)
		String broadcast(BroadcastRequest request);
	}
	
	interface BroadcastServiceWithIllegalReducer {
		@AstrixBroadcast(reducer = StringToStringReducer.class)
		Future<String> broadcast(BroadcastRequest request);
	}
	
	public static class BroadcastReducer implements RemoteResultReducer<String> {
		@Override
		public String reduce(List<AstrixRemoteResult<String>> result) {
			return result.get(0).getResult(); // Only one 'partition'
		}
	}
	
	public static class GenericReducer<T> implements RemoteResultReducer<T> {

		@Override
		public T reduce(List<AstrixRemoteResult<T>> result) {
			return result.get(0).getResult();
		}
		
	}
	
	public static class StringToStringReducer implements RemoteResultReducer<String> {
		@Override
		public String reduce(List<AstrixRemoteResult<String>> result) {
			return null; // Never invoked, 
		}
	}
	
	public static class DateToDateReducer implements RemoteResultReducer<Date> {
		@Override
		public Date reduce(List<AstrixRemoteResult<Date>> result) {
			return null; // Never invoked, 
		}
	}
	
	public static class MyCustomServiceException extends ServiceInvocationException {

		private static final long serialVersionUID = 1L;
		
		public MyCustomServiceException() {
			super("my-custom-message");
		}

		public MyCustomServiceException(String msg) {
			super(msg);
		}
		
		@Override
		public ServiceInvocationException recreateOnClientSide() {
			return new MyCustomServiceException(getMessage());
		}
		
	}
	
	private static class PartitionedDirectTransport implements RemotingTransportSpi {

		private List<AstrixServiceActivator> partitions;
		
		public PartitionedDirectTransport(List<AstrixServiceActivator> partitions) {
			this.partitions = partitions;
		}

		@Override
		public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey){
			final AstrixServiceInvocationResponse response = getActivator(routingKey).invokeService(request);
			return Observable.create(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(Subscriber<? super AstrixServiceInvocationResponse> t1) {
					t1.onNext(response);
					t1.onCompleted();
				}
			});
		}

		private AstrixServiceActivator getActivator(RoutingKey routingKey) {
			return partitions.get(routingKey.hashCode() % partitions.size());
		}

		@Override
		public Observable<AstrixServiceInvocationResponse> submitBroadcastRequest(AstrixServiceInvocationRequest request) {
			final List<AstrixServiceInvocationResponse> responses = new ArrayList<>();
			for (AstrixServiceActivator partition : partitions) {
				responses.add(partition.invokeService(request));
			}
			return Observable.create(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(Subscriber<? super AstrixServiceInvocationResponse> t1) {
					for (AstrixServiceInvocationResponse r : responses) {
						t1.onNext(r);
					}
					t1.onCompleted();
				}
			});
			
		}

		@Override
		public int partitionCount() {
			return this.partitions.size();
		}

		@Override
		public Observable<AstrixServiceInvocationResponse> submitRoutedRequests(Collection<RoutedServiceInvocationRequest> requests) {
			Observable<AstrixServiceInvocationResponse> result = Observable.empty();
			for (RoutedServiceInvocationRequest request : requests) {
				result = result.mergeWith(submitRoutedRequest(request.getRequest(), request.getRoutingkey()));
			}
			return result;
		}
		
	}
	
}
