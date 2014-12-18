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
package com.avanza.astrix.remoting.server;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;

import com.avanza.astrix.context.JavaSerializationSerializer;
import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRemoteResultReducer;
import com.avanza.astrix.remoting.client.AstrixMissingServiceException;
import com.avanza.astrix.remoting.client.AstrixRemoteServiceException;
import com.avanza.astrix.remoting.client.AstrixRemotingProxy;
import com.avanza.astrix.remoting.client.AstrixRemotingTransport;
import com.avanza.astrix.remoting.client.Router;
import com.avanza.astrix.remoting.client.RoutingKey;
import com.avanza.astrix.remoting.client.RoutingStrategy;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceActivatorTest {

	AstrixObjectSerializer objectSerializer = new JavaSerializationSerializer(1);
	AstrixServiceActivator activator = new AstrixServiceActivator();
	
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
		activator.register(impl, objectSerializer, TestService.class);
		
		TestService testService = AstrixRemotingProxy.create(TestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

		HelloRequest request = new HelloRequest("kalle");
		HelloResponse reply = testService.hello(request);
		assertEquals("reply-kalle", reply.getGreeting());
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
		
		activator.register(impl, objectSerializer, TestService.class);
		
		TestService testService = AstrixRemotingProxy.create(TestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

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
		activator.register(serviceProvider, objectSerializer, AnnotatedArgumentTestService.class);
		
		AnnotatedArgumentTestService serviceProxy = AstrixRemotingProxy.create(AnnotatedArgumentTestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

		String reply = serviceProxy.hello("kalle", "hello-");
		assertEquals("hello-kalle", reply);
	}
	
	
	@Test
	public void routedRequest_throwsException() throws Exception {
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
			activator.register(impl, objectSerializer, TestService.class);

			TestService testService = AstrixRemotingProxy.create(TestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
			testService.hello(new HelloRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (AstrixRemoteServiceException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("Remote service threw exception, see server log for details. [java.lang.IllegalArgumentException: Remote service error message]"));
		}
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
			activator.register(impl, objectSerializer, BroadcastService.class);

			BroadcastService broadcastService = AstrixRemotingProxy.create(BroadcastService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
			broadcastService.broadcast(new BroadcastRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (AstrixRemoteServiceException e) {
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
		activator.register(provider, objectSerializer, BroadcastService.class);
		activator.register(provider, objectSerializer, TestService.class);

		BroadcastService broadcastService = AstrixRemotingProxy.create(BroadcastService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
		TestService testService = AstrixRemotingProxy.create(TestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
		
		assertEquals(provider.hello(new HelloRequest("kalle")), testService.hello(new HelloRequest("kalle")));
		assertEquals(provider.broadcast(new BroadcastRequest("kalle")), broadcastService.broadcast(new BroadcastRequest("kalle")));
	}
	
	@Test
	public void request_NoCorrespondingService_throwsException() throws Exception {
		try {
			TestService missingRemoteService = AstrixRemotingProxy.create(TestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
			missingRemoteService.hello(new HelloRequest("foo"));
		} catch (AstrixRemoteServiceException e) {
			assertEquals(AstrixMissingServiceException.class.getName(), e.getExceptionType());
		}
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
		activator.register(impl, objectSerializer, TestService.class);
		
		ObservableTestService service = AstrixRemotingProxy.create(ObservableTestService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
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
		activator.register(impl, objectSerializer, TestService.class);
		
		TestServiceAsync service = AstrixRemotingProxy.create(TestServiceAsync.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());
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
		activator = new AstrixServiceActivator();
		
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
		activator.register(impl, objectSerializer, TestService.class);
		
		
		ObservableTestService service = AstrixRemotingProxy.create(ObservableTestService.class, AstrixRemotingTransport.direct(activator), corruptDeserializer, new NoRoutingStrategy());
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
		activator.register(impl, objectSerializer, GenericReturnTypeService.class);
		
		GenericReturnTypeService testService = AstrixRemotingProxy.create(GenericReturnTypeService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

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
		activator.register(impl, objectSerializer, BroadcastingGenericReturnTypeService.class);
		
		BroadcastingGenericReturnTypeService testService = AstrixRemotingProxy.create(BroadcastingGenericReturnTypeService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

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
		activator.register(impl, objectSerializer, NoArgumentService.class);
		
		NoArgumentService testService = AstrixRemotingProxy.create(NoArgumentService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

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
		activator.register(impl, objectSerializer, VoidService.class);
		
		VoidService testService = AstrixRemotingProxy.create(VoidService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

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
		activator.register(impl, objectSerializer, BroadcastVoidService.class);
		
		BroadcastVoidService testService = AstrixRemotingProxy.create(BroadcastVoidService.class, AstrixRemotingTransport.direct(activator), objectSerializer, new NoRoutingStrategy());

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(0, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWhenRegisteringProviderForNonImplementedInterface() throws Exception {
		activator.register(new Object(), objectSerializer, TestService.class);
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
		@AstrixBroadcast(reducer = BroadcastReducer.class)
		String broadcast(BroadcastRequest request);
	}
	
	public static class BroadcastReducer implements AstrixRemoteResultReducer<String, String> {
		@Override
		public String reduce(List<AstrixRemoteResult<String>> result) {
			return result.get(0).getResult(); // Only one 'partition'
		}

	}
	
}
