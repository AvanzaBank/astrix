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
import static org.junit.Assert.assertArrayEquals;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixPartitionedRouting;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRoutingStrategy;
import com.avanza.astrix.core.RemoteResultReducer;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceInvocationException;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.remoting.Router;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.remoting.client.IncompatibleRemoteResultReducerException;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;

import rx.Observable;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixRemotingTest {

	@Test
	public void supportsServiceMethodsWithMultipleArguments() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		TwoArgumentTest impl = new TwoArgumentTest() {
			@Override
			public String hello(HelloRequest message, String greeting) {
				return greeting + message.getMesssage();
			}
		};
		remotingDriver.registerServer(TwoArgumentTest.class, impl);

		TwoArgumentTest testService = remotingDriver.createRemotingProxy(TwoArgumentTest.class);

		HelloRequest request = new HelloRequest("kalle");
		String reply = testService.hello(request, "replyTo-");
		assertEquals("replyTo-kalle", reply);
	}
	
	@Test
	public void routedRequest_throwsExceptionOfNonServiceInvocationExceptionType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		try {
			TestService impl = new TestService() {
				@Override
				public HelloResponse hello(HelloRequest message) {
					throw new IllegalArgumentException("Remote service error message");
				}
			};
			remotingDriver.registerServer(TestService.class, impl);

			TestService testService = remotingDriver.createRemotingProxy(TestService.class);
			testService.hello(new HelloRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (RemoteServiceInvocationException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Remote service error message]"));
		}
	}
	
	@Test
	public void routedRequest_throwsExceptionOfServiceInvocationType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		try {
			TestService impl = new TestService() {
				@Override
				public HelloResponse hello(HelloRequest message) {
					throw new MyCustomServiceException();
				}
			};
			remotingDriver.registerServer(TestService.class, impl);

			TestService testService = remotingDriver.createRemotingProxy(TestService.class);
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
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PingService impl = new PingService() {
			@Override
			public List<String> ping(String msg) {
				return Arrays.asList(msg);
			}
		};
		remotingDriver.registerServerPartition(0, PingService.class, impl);
		remotingDriver.registerServerPartition(1, PingService.class, impl);

		PingService broadcastService = remotingDriver.createRemotingProxy(PingService.class);
		List<String> replys = broadcastService.ping("foo");
		assertEquals(2, replys.size());
		assertEquals("foo", replys.get(0));
		assertEquals("foo", replys.get(1));
	}
	
	@Test
	public void partitionedRequest() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
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
		
		remotingDriver.registerServerPartition(0, CalculatorListService.class, eventPartitionCalculator);
		remotingDriver.registerServerPartition(1, CalculatorListService.class, oddPartitionCalculator);

		CalculatorListService calculatorService = remotingDriver.createRemotingProxy(CalculatorListService.class);
		int squareSum = calculatorService.squareSum(Arrays.asList(1, 2, 3, 4, 5));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test
	public void customRoutingRequest() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		CustomRoutedCalc evenPartitionCalculator = new CustomRoutedCalc() {
			@Override
			public int squareSum(Collection<Integer> nums) {
				int squareSum = 0;
				for (int num : nums) {
					squareSum += num * num;
				}
				return squareSum;
			}
		};
		CustomRoutedCalc oddPartitionCalculator = new CustomRoutedCalc() {
			@Override
			public int squareSum(Collection<Integer> nums) {
				throw new AssertionError("All request should be statically routed to even partition using custom router");
			}
		};
		
		remotingDriver.registerServerPartition(0, CustomRoutedCalc.class, evenPartitionCalculator);
		remotingDriver.registerServerPartition(1, CustomRoutedCalc.class, oddPartitionCalculator);

		CustomRoutedCalc calculatorService = remotingDriver.createRemotingProxy(CustomRoutedCalc.class);
		int squareSum = calculatorService.squareSum(Arrays.asList(1, 2, 3, 4, 5));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}

	@Test
	public void partitionedRequest_GenericArrayArgument() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingService eventPartitionPing = new PartitionedPingServiceImpl();
		PartitionedPingService oddPartitionPing = new PartitionedPingServiceImpl();
		
		remotingDriver.registerServerPartition(0, PartitionedPingService.class, eventPartitionPing);
		remotingDriver.registerServerPartition(1, PartitionedPingService.class, oddPartitionPing);

		PartitionedPingService partitionedPing = remotingDriver.createRemotingProxy(PartitionedPingService.class);
		assertThat(partitionedPing.ping(new double[]{1d, 2d, 3d}), containsInAnyOrder(1d, 2d, 3d));
		assertThat(partitionedPing.ping(new int[]{1, 2, 3}), containsInAnyOrder(1, 2, 3));
		assertThat(partitionedPing.ping(new float[]{1f, 2f, 3f}), containsInAnyOrder(1f, 2f, 3f));
		assertThat(partitionedPing.ping(new short[]{1, 2, 3}), containsInAnyOrder(new Short[]{1, 2, 3}));
		assertThat(partitionedPing.ping(new long[]{1L, 2L, 3L}), containsInAnyOrder(1L, 2L, 3L));
		assertThat(partitionedPing.ping(new String[]{"1", "2", "3"}), containsInAnyOrder("1", "2", "3"));
	}
	
	@Test
	public void partitionedRequest_voidReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingServiceImpl evenPartitionPing = new PartitionedPingServiceImpl();
		PartitionedPingServiceImpl oddPartitionPing = new PartitionedPingServiceImpl();
		
		remotingDriver.registerServerPartition(0, PartitionedPingService.class, evenPartitionPing);
		remotingDriver.registerServerPartition(1, PartitionedPingService.class, oddPartitionPing);

		PartitionedPingService partitionedPing = remotingDriver.createRemotingProxy(PartitionedPingService.class);
		partitionedPing.pingVoid(new Integer[]{1, 2, 3});
		
		assertArrayEquals(new Integer[]{1, 3}, oddPartitionPing.lastAsyncPingRequest);
		assertArrayEquals(new Integer[]{2}, evenPartitionPing.lastAsyncPingRequest);
	}
	
	@Test
	public void asyncPartitionedRequest_voidReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingServiceImpl evenPartitionPing = new PartitionedPingServiceImpl();
		PartitionedPingServiceImpl oddPartitionPing = new PartitionedPingServiceImpl();

		remotingDriver.registerServerPartition(0, PartitionedPingService.class, evenPartitionPing);
		remotingDriver.registerServerPartition(1, PartitionedPingService.class, oddPartitionPing);

		PartitionedPingServiceAsync partitionedPing = remotingDriver.createRemotingProxy(PartitionedPingServiceAsync.class, PartitionedPingService.class);
		partitionedPing.pingVoid(new Integer[]{1, 2, 3}).subscribe();

		assertArrayEquals(new Integer[]{1, 3}, oddPartitionPing.lastAsyncPingRequest);
		assertArrayEquals(new Integer[]{2}, evenPartitionPing.lastAsyncPingRequest);
	}

	@Test
	public void partitionedRequest_emptyArgument() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingService evenPartitionPing = Mockito.mock(PartitionedPingService.class);
		PartitionedPingService oddPartitionPing = Mockito.mock(PartitionedPingService.class);
		
		remotingDriver.registerServerPartition(0, PartitionedPingService.class, evenPartitionPing);
		remotingDriver.registerServerPartition(1, PartitionedPingService.class, oddPartitionPing);

		PartitionedPingService partitionedPing = remotingDriver.createRemotingProxy(PartitionedPingService.class, PartitionedPingService.class); 
		partitionedPing.pingVoid(new Integer[]{});
		Mockito.verifyZeroInteractions(evenPartitionPing, oddPartitionPing);
	}
	
	@Test(expected = RemoteServiceInvocationException.class)
	public void partitionedRoutingRequest_NonServiceInovcationExcpetion_WrappedInRemoteServiceInvocation() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingService evenPartitionPing = new PartitionedPingServiceImpl() {
			@Override
			public List<String> ping(String... nums) {
				throw new NullPointerException();
			};
		};
		PartitionedPingService oddPartitionPing = new PartitionedPingServiceImpl();
		
		remotingDriver.registerServerPartition(0, PartitionedPingService.class, evenPartitionPing);
		remotingDriver.registerServerPartition(1, PartitionedPingService.class, oddPartitionPing);

		PartitionedPingService partitionedPing = remotingDriver.createRemotingProxy(PartitionedPingService.class, PartitionedPingService.class);
		partitionedPing.ping(new String[]{"1", "2", "3", "4", "5"});
	}

	@Test(expected = MyCustomServiceException.class)
	public void partitionedRoutingRequest_voidReturnType_throwsException() throws Exception{
		AstrixRemotingDriver astrixRemotingDriver = new AstrixRemotingDriver(2);
		PartitionedPingServiceImpl evenPartition = new PartitionedPingServiceImpl() {
			@Override
			public void pingVoid(Integer... nums) {
				throw new MyCustomServiceException();
			}
		};

		PartitionedPingServiceImpl oddPartition = new PartitionedPingServiceImpl();
		astrixRemotingDriver.registerServerPartition(0, PartitionedPingService.class, evenPartition);
		astrixRemotingDriver.registerServerPartition(1,PartitionedPingService.class, oddPartition);
		PartitionedPingService partitionedService = astrixRemotingDriver.createRemotingProxy(PartitionedPingService.class);
		partitionedService.pingVoid(1,2,3,4);

	}
	
	@Test
	public void partitionedRequest_routingOnPropertyOnTargetObject() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		CalculatorArrayPojoService evenPartitionCalculator = new CalculatorArrayPojoService() {
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
		
		remotingDriver.registerServerPartition(0, CalculatorArrayPojoService.class, evenPartitionCalculator);
		remotingDriver.registerServerPartition(1, CalculatorArrayPojoService.class, oddPartitionCalculator);

		CalculatorArrayPojoService calculatorService = remotingDriver.createRemotingProxy(CalculatorArrayPojoService.class, CalculatorArrayPojoService.class);
		int squareSum = calculatorService.squareSum(new NumPojo(1), new NumPojo(2), new NumPojo(3), new NumPojo(4), new NumPojo(5));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test
	public void partitionedRequest_routingOnPropertyOnTargetObject_CollectionArgument() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(2);
		CalculatorListPojoService eventPartitionCalculator = new CalculatorListPojoServiceImpl();
		CalculatorListPojoService oddPartitionCalculator = new CalculatorListPojoServiceImpl();
		
		remotingDriver.registerServerPartition(0, CalculatorListPojoService.class, eventPartitionCalculator);
		remotingDriver.registerServerPartition(1, CalculatorListPojoService.class, oddPartitionCalculator);

		CalculatorListPojoService calculatorService = remotingDriver.createRemotingProxy(CalculatorListPojoService.class);
		int squareSum = calculatorService.squareSum(Arrays.asList(new NumPojo(1), new NumPojo(2), new NumPojo(3), new NumPojo(4), new NumPojo(5)));
		assertEquals(1 + 4 + 9 + 16 + 25, squareSum);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedRequest_routingOnProperty_throwsExceptionForRawTypes() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.createRemotingProxy(ServiceWithRawListRoutingArgument.class); 
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedRequest_routingOnProperty_throwsExceptionForMissingMethods() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.createRemotingProxy(ServiceWithListMissingRoutingPropertyMethod.class);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void partitionedService_IncompatibleCollectionType_throwsException() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.createRemotingProxy(InvalidCollectionTypePartitionedService.class); 
	}
	
	@Test(expected = IncompatibleRemoteResultReducerException.class)
	public void partitionedService_IncompatibleReducer_throwsException() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.createRemotingProxy(InvalidReducerPartitionedService.class);
	}
	
	@Test
	public void partitionedService_NonListCollection() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		PartitionedServiceUsingSet sevenPartitionService = new PartitionedServiceUsingSet() {
			@Override
			public Set<Integer> ping(Set<Integer> nums) {
				return nums;
			}
		};
		
		remotingDriver.registerServer(PartitionedServiceUsingSet.class, sevenPartitionService);

		PartitionedServiceUsingSet calculatorService = remotingDriver.createRemotingProxy(PartitionedServiceUsingSet.class); 
		assertEquals(setOf(1,2,3), calculatorService.ping(setOf(1,2,3)));
	}
	
	private static <T> Set<T> setOf(@SuppressWarnings("unchecked") T... values) {
		return new HashSet<T>(Arrays.asList(values));
	}

	@Test
	public void broadcastRequest_throwsException() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		try {
			BroadcastService impl = new BroadcastService() {
				@Override
				public String broadcast(BroadcastRequest request) {
					throw new IllegalArgumentException("Broadcast error message");
				}
			};
			remotingDriver.registerServer(BroadcastService.class, impl);

			BroadcastService broadcastService = remotingDriver.createRemotingProxy(BroadcastService.class);
			broadcastService.broadcast(new BroadcastRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (RemoteServiceInvocationException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Broadcast error message]"));
		}
	}
	
	@Test
	public void publishMultipleApis() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		class MultiProvider implements BroadcastService, TestService {

			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("hello " + message.getMesssage());
			}

			@Override
			@AstrixBroadcast(reducer = BroadcastReducer.class)
			public String broadcast(BroadcastRequest request) {
				return "broadcast-" + request.getMesssage();
			}
			
		}
		MultiProvider provider = new MultiProvider();
		remotingDriver.registerServer(BroadcastService.class, provider);
		remotingDriver.registerServer(TestService.class, provider);

		BroadcastService broadcastService = remotingDriver.createRemotingProxy(BroadcastService.class);
		TestService testService = remotingDriver.createRemotingProxy(TestService.class);
		
		assertEquals(provider.hello(new HelloRequest("kalle")), testService.hello(new HelloRequest("kalle")));
		assertEquals(provider.broadcast(new BroadcastRequest("kalle")), broadcastService.broadcast(new BroadcastRequest("kalle")));
	}
	
	@Test(expected = ServiceUnavailableException.class)
	public void request_NoCorrespondingServiceRegisteredInServiceActivator_throwsServiceUnavailableException() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		TestService missingRemoteService = remotingDriver.createRemotingProxy(TestService.class);
		missingRemoteService.hello(new HelloRequest("foo"));
	}
	
	@Test
	public void useObservableVersionOfAService() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
		};
		remotingDriver.registerServer(TestService.class, impl);
		
		ObservableTestService service = remotingDriver.createRemotingProxy(ObservableTestService.class, TestService.class);
		Observable<HelloResponse> message = service.hello(new HelloRequest("kalle"));
		assertEquals("reply-kalle", message.toBlocking().first().getGreeting());
	}
	
	@Test
	public void useAsyncVersionOfAService() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
		};
		remotingDriver.registerServer(TestService.class, impl);
		
		TestServiceAsync service = remotingDriver.createRemotingProxy(TestServiceAsync.class, TestService.class);
		Future<HelloResponse> response = service.hello(new HelloRequest("kalle"));
		assertEquals("reply-kalle", response.get().getGreeting());
	}
	
	@Test(expected = RuntimeException.class)
	public void ioExceptionThrownDuringDeserializationAreProppagatedAsRuntimeExceptions() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
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
		
		TestService impl = new TestService() {
			@Override
			public HelloResponse hello(HelloRequest message) {
				return new HelloResponse("reply-" + message.getMesssage());
			}
		};
		remotingDriver.registerServer(TestService.class, impl);
		
		
		ObservableTestService service = remotingDriver.createRemotingProxy(ObservableTestService.class, TestService.class, corruptDeserializer);
		Observable<HelloResponse> message = service.hello(new HelloRequest("kalle"));
		message.toBlocking().first();
	}
	
	@Test
	public void supportServicesThatAcceptAndReturnGenericTypes() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
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
		remotingDriver.registerServer(GenericReturnTypeService.class, impl);
		
		GenericReturnTypeService testService = remotingDriver.createRemotingProxy(GenericReturnTypeService.class, GenericReturnTypeService.class);

		HelloRequest request = new HelloRequest("kalle");
		List<HelloResponse> reply = testService.hello("foo-routing", Arrays.<HelloRequest>asList(request));
		assertEquals(1, reply.size());
		assertEquals("reply-kalle", reply.get(0).getGreeting());
	}
	
	@Test
	public void supportServicesThatAcceptAndReturnGenericOnBroadcast() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
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
		remotingDriver.registerServer(BroadcastingGenericReturnTypeService.class, impl);
		
		BroadcastingGenericReturnTypeService testService = remotingDriver.createRemotingProxy(BroadcastingGenericReturnTypeService.class);

		HelloRequest request = new HelloRequest("kalle");
		List<HelloResponse> reply = testService.hello(Arrays.<HelloRequest>asList(request));
		assertEquals(1, reply.size());
		assertEquals("reply-kalle", reply.get(0).getGreeting());
	}
	
	@Test
	public void supportServicesWithNoArgument() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		NoArgumentService impl = new NoArgumentService() {
			@Override
			public List<String> hello() {
				return Arrays.asList("response");
			}
		};
		remotingDriver.registerServer(NoArgumentService.class, impl);
		
		NoArgumentService testService = remotingDriver.createRemotingProxy(NoArgumentService.class);
		List<String> reply = testService.hello();
		assertEquals(1, reply.size());
		assertEquals("response", reply.get(0));
	}
	
	@Test
	public void supportsServicesThatWithVoidReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		final BlockingQueue<String> receivedRequest = new LinkedBlockingQueue<>();
		VoidService impl = new VoidService() {
			@Override
			public void hello(String message) {
				receivedRequest.add(message);
			}
		};
		remotingDriver.registerServer(VoidService.class, impl);
		
		VoidService testService = remotingDriver.createRemotingProxy(VoidService.class);

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(1, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test
	public void supportsBroadcastedServicesWithVoidReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		final BlockingQueue<String> receivedRequest = new LinkedBlockingQueue<>();
		BroadcastVoidService impl = new BroadcastVoidService() {
			@Override
			public void hello(String message) {
				receivedRequest.add(message);
			}
		};
		remotingDriver.registerServer(BroadcastVoidService.class, impl);
		
		BroadcastVoidService testService = remotingDriver.createRemotingProxy(BroadcastVoidService.class);

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(0, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}

	@Test
	public void supportsAsyncBroadcastedServicesWithVoidReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		final BlockingQueue<String> receivedRequest = new LinkedBlockingQueue<>();
		BroadcastVoidService impl = new BroadcastVoidService() {
			@Override
			public void hello(String message) {
				receivedRequest.add(message);
			}
		};
		remotingDriver.registerServer(BroadcastVoidService.class, impl);

		BroadcastVoidServiceAsync testService = remotingDriver.createRemotingProxy(BroadcastVoidServiceAsync.class, BroadcastVoidService.class);

		testService.hello("kalle").subscribe();
		String lastReceivedRequest = receivedRequest.poll(0, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test(expected = MyCustomServiceException.class)
	public void supports_BroadcastedServicesWithVoidReturnType_throwingExceptions() throws Exception{
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		BroadcastVoidService impl = new BroadcastVoidService(){

			@Override
			public void hello(String message) {
				throw new MyCustomServiceException();
			}
		};
		remotingDriver.registerServer(BroadcastVoidService.class, impl);

		BroadcastVoidService voideService = remotingDriver.createRemotingProxy(BroadcastVoidService.class);

		voideService.hello("test");
	}


	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWhenRegisteringProviderForNonImplementedInterface() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(TestService.class, new Object());
	}
	
	@Test
	public void remotingProxiesDoesNotDelegateMethodCallsForMethodsDefinedIn_java_lang_Object() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(VoidService.class, new VoidService() {
			@Override
			public void hello(String message) {
			}
			@Override
			public String toString() {
				return "RemotingServiceToString";
			}
		});
		
		VoidService testService = remotingDriver.createRemotingProxy(VoidService.class);
		assertEquals("RemotingProxy[" + VoidService.class.getName() + "]", testService.toString());
	}
	
	@Test
	public void asyncBroadcastedService() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(BroadcastingGenericReturnTypeService.class, new BroadcastingGenericReturnTypeService() {
			@Override
			public List<HelloResponse> hello(List<HelloRequest> greeting) {
				return Arrays.asList(new HelloResponse(greeting.get(0).getMesssage()));
			}
			
		});
		
		BroadcastingGenericReturnTypeServiceAsync service = remotingDriver.createRemotingProxy(BroadcastingGenericReturnTypeServiceAsync.class,
																							   BroadcastingGenericReturnTypeService.class);
		Future<List<HelloResponse>> resultFuture = service.hello(Arrays.asList(new HelloRequest("foo")));
		List<HelloResponse> result = resultFuture.get();
		assertEquals(1, result.size());
	}
	
	@Test(expected = IncompatibleRemoteResultReducerException.class)
	public void throwsExceptionOnProxyCreationIfRemoteResultReducerDoesNotHaveAMethodSignatureCompatibleWithServiceMethodSignature() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.createRemotingProxy(BroadcastServiceWithIllegalReducer.class); 
	}
	
	@Test(expected = IllegalStateException.class)
	public void throwsIllegalStateExceptionIfRoutingStrategyReturnsNull() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(VoidService.class, new VoidService() {
			@Override
			public void hello(String message) {
			}
		});
		
		VoidService voidService = remotingDriver.createRemotingProxy(VoidService.class, VoidService.class, new RoutingStrategy() {
			@Override
			public Router create(Method serviceMethod) {
				return args -> null;
			}
		});
		voidService.hello("foo");
	}
	
	@Test
	public void supportsOptionalReturnType() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(OptionalPing.class, new OptionalPing() {
			@Override
			public Optional<String> ping(String message) {
				return Optional.ofNullable(message);
			}
		});
		
		OptionalPing pingService = remotingDriver.createRemotingProxy(OptionalPing.class);

		assertEquals("foo", pingService.ping("foo").get());
		assertEquals(Optional.empty(), pingService.ping(null));
		
		assertEquals("foo", pingService.broadcastPing("foo").get());
		assertEquals(Optional.empty(), pingService.broadcastPing(null));
	}
	
	@Test
	public void supportsOptionalWithNullReturnValue() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(OptionalPing.class, new OptionalPing() {
			@Override
			public Optional<String> ping(String message) {
				return null;
			}
		});
		
		OptionalPing pingService = remotingDriver.createRemotingProxy(OptionalPing.class);

		assertEquals(null, pingService.ping("foo"));
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

	public interface ServiceWithRawListRoutingArgument {
		@SuppressWarnings("rawtypes")
		List<Integer> foo(@AstrixPartitionedRouting(routingMethod = "getRoutingKey") List list);
	}
	
	public interface ServiceWithListMissingRoutingPropertyMethod {
		List<Integer> foo(@AstrixPartitionedRouting(routingMethod = "aMissingMethod") List<NumPojo> list);
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
		void pingVoid(@AstrixPartitionedRouting Integer... nums);
	}
	
	interface PartitionedPingServiceAsync {
		Observable<Void> pingVoid(@AstrixPartitionedRouting Integer... nums);
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

		Integer[] lastAsyncPingRequest;
		
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
		public void pingVoid(Integer... nums) {
			this.lastAsyncPingRequest = nums;
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
	}
	
	interface TwoArgumentTest {
		String hello(HelloRequest message, String greeting);
	}
	
	public static class StaticRouting implements RoutingStrategy {
		@Override
		public Router create(Method serviceMethod) {
			return new Router() {
				@Override
				public RoutingKey getRoutingKey(Object[] args) throws Exception {
					return RoutingKey.create(0);
				}
			};
		}
		
	}
	
	interface CustomRoutedCalc {
		@AstrixRoutingStrategy(StaticRouting.class)
		int squareSum(Collection<Integer> nums);
		
	}
	
	interface VoidService {
		void hello(String message);
	}
	
	interface OptionalPing {
		Optional<String> ping(String message);
		@AstrixBroadcast(reducer = FirstNonEmpty.class)
		default Optional<String> broadcastPing(String message) {
			return ping(message);
		}
	}
	
	public static class FirstNonEmpty implements RemoteResultReducer<Optional<String>> {

		@Override
		public Optional<String> reduce(List<AstrixRemoteResult<Optional<String>>> result) {
			for (AstrixRemoteResult<Optional<String>> r : result) {
				if (r.getResult().isPresent()) {
					return r.getResult();
				}
			}
			return Optional.empty();
		}
		
	}
	
	interface BroadcastVoidService {
		@AstrixBroadcast
		void hello(String message);
	}

	interface BroadcastVoidServiceAsync {
		@AstrixBroadcast
		Observable<Void> hello(String message);
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
	
}
