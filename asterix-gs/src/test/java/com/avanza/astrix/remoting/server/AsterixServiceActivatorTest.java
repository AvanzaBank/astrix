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
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openspaces.remoting.Routing;

import rx.Observable;

import com.avanza.astrix.context.AsterixApiDescriptor;
import com.avanza.astrix.core.AsterixBroadcast;
import com.avanza.astrix.core.AsterixObjectSerializer;
import com.avanza.astrix.core.AsterixRemoteResult;
import com.avanza.astrix.core.AsterixRemoteResultReducer;
import com.avanza.astrix.provider.versioning.AsterixObjectMapperConfigurer;
import com.avanza.astrix.provider.versioning.AsterixVersioned;
import com.avanza.astrix.provider.versioning.JacksonObjectMapperBuilder;
import com.avanza.astrix.remoting.client.AsterixMissingServiceException;
import com.avanza.astrix.remoting.client.AsterixRemoteServiceException;
import com.avanza.astrix.remoting.client.AsterixRemotingProxy;
import com.avanza.astrix.remoting.client.AsterixRemotingTransport;
import com.avanza.astrix.remoting.server.AsterixServiceActivator;
import com.avanza.astrix.versioning.plugin.JacksonVersioningPlugin;
import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceActivatorTest {

	public static class DummyConfigurer implements AsterixObjectMapperConfigurer {
		@Override
		public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
		}
	}
	
	@AsterixVersioned(
			apiMigrations = {},
			objectMapperConfigurer = DummyConfigurer.class,
			version = 1
	)
	public static class DummyDescriptor {
		
	}
	
	AsterixObjectSerializer objectSerializer = new JacksonVersioningPlugin().create(AsterixApiDescriptor.create(DummyDescriptor.class));
	AsterixServiceActivator activator = new AsterixServiceActivator();
	
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
		
		TestService testService = AsterixRemotingProxy.create(TestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		TestService testService = AsterixRemotingProxy.create(TestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		AnnotatedArgumentTestService serviceProxy = AsterixRemotingProxy.create(AnnotatedArgumentTestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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

			TestService testService = AsterixRemotingProxy.create(TestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
			testService.hello(new HelloRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (AsterixRemoteServiceException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("[java.lang.IllegalArgumentException: Remote service error message]"));
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

			BroadcastService broadcastService = AsterixRemotingProxy.create(BroadcastService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
			broadcastService.broadcast(new BroadcastRequest("foo"));
			fail("Expected remote service exception to be thrown");
		} catch (AsterixRemoteServiceException e) {
			assertEquals(IllegalArgumentException.class.getName(), e.getExceptionType());
			assertThat(e.getMessage(), startsWith("[java.lang.IllegalArgumentException: Broadcast error message]"));
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
			@AsterixBroadcast(reducer = BroadcastReducer.class)
			public String broadcast(BroadcastRequest request) {
				return "broadcast-" + request.getMesssage();
			}
			
		}
		MultiProvider provider = new MultiProvider();
		activator.register(provider, objectSerializer, BroadcastService.class);
		activator.register(provider, objectSerializer, TestService.class);

		BroadcastService broadcastService = AsterixRemotingProxy.create(BroadcastService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
		TestService testService = AsterixRemotingProxy.create(TestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
		
		assertEquals(provider.hello(new HelloRequest("kalle")), testService.hello(new HelloRequest("kalle")));
		assertEquals(provider.broadcast(new BroadcastRequest("kalle")), broadcastService.broadcast(new BroadcastRequest("kalle")));
	}
	
	@Test
	public void request_NoCorrespondingService_throwsException() throws Exception {
		try {
			TestService missingRemoteService = AsterixRemotingProxy.create(TestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
			missingRemoteService.hello(new HelloRequest("foo"));
		} catch (AsterixRemoteServiceException e) {
			assertEquals(AsterixMissingServiceException.class.getName(), e.getExceptionType());
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
		
		ObservableTestService service = AsterixRemotingProxy.create(ObservableTestService.class, AsterixRemotingTransport.direct(activator), objectSerializer);
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
		
		TestServiceAsync service = AsterixRemotingProxy.create(TestServiceAsync.class, AsterixRemotingTransport.direct(activator), objectSerializer);
		Future<HelloResponse> response = service.hello(new HelloRequest("kalle"));
		assertEquals("reply-kalle", response.get().getGreeting());
	}
	
	@Test(expected = RuntimeException.class)
	public void ioExceptionThrownDuringDeserializationAreProppagatedAsRuntimeExceptions() throws Exception {
		AsterixObjectSerializer corruptDeserializer = new AsterixObjectSerializer.NoVersioningSupport() {
			@Override
			public <T> T deserialize(Object element, Type target, int version) {
				if (target.equals(HelloResponse.class)) {
					// simulate failure in deserializing service invocation response
					throw new IllegalArgumentException("phew.. I/O, huh?");
				}
				return super.deserialize(element, target, version);
			}
			
		};
		activator = new AsterixServiceActivator();
		
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
		
		
		ObservableTestService service = AsterixRemotingProxy.create(ObservableTestService.class, AsterixRemotingTransport.direct(activator), corruptDeserializer);
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
		
		GenericReturnTypeService testService = AsterixRemotingProxy.create(GenericReturnTypeService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		BroadcastingGenericReturnTypeService testService = AsterixRemotingProxy.create(BroadcastingGenericReturnTypeService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		NoArgumentService testService = AsterixRemotingProxy.create(NoArgumentService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		VoidService testService = AsterixRemotingProxy.create(VoidService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

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
		
		BroadcastVoidService testService = AsterixRemotingProxy.create(BroadcastVoidService.class, AsterixRemotingTransport.direct(activator), objectSerializer);

		testService.hello("kalle");
		String lastReceivedRequest = receivedRequest.poll(0, TimeUnit.SECONDS);
		assertEquals("kalle", lastReceivedRequest);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionWhenRegisteringProviderForNonImplementedInterface() throws Exception {
		activator.register(new Object(), objectSerializer, TestService.class);
	}
	
	public static class HelloRequest {
		private String messsage;
		
		public HelloRequest(String messsage) {
			this.messsage = messsage;
		}
		
		public HelloRequest() {
		}
		
		@SpaceRouting
		public String getMesssage() {
			return messsage;
		}

		public void setMesssage(String messsage) {
			this.messsage = messsage;
		}
	}
	
	public static class BroadcastRequest {
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
	
	public static class HelloResponse {
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
		@AsterixBroadcast
		List<String> hello();
	}
	
	interface GenericReturnTypeService {
		List<HelloResponse> hello(@Routing String routingKey, List<HelloRequest> greeting);
	}
	
	interface BroadcastingGenericReturnTypeService {
		@AsterixBroadcast
		List<HelloResponse> hello(List<HelloRequest> greeting);
	}
	
	interface TestService {
		HelloResponse hello(HelloRequest message);
		String hello(HelloRequest message, String greeting);
	}
	
	interface VoidService {
		void hello(@Routing String message);
	}
	
	interface BroadcastVoidService {
		@AsterixBroadcast
		void hello(String message);
	}
	
	interface AnnotatedArgumentTestService {
		String hello(@Routing String message, String greeting);
	}
	
	interface ObservableTestService {
		Observable<HelloResponse> hello(HelloRequest message);
	}
	
	interface TestServiceAsync {
		Future<HelloResponse> hello(HelloRequest message);
	}
	
	interface BroadcastService {
		@AsterixBroadcast(reducer = BroadcastReducer.class)
		String broadcast(BroadcastRequest request);
	}
	
	public static class BroadcastReducer implements AsterixRemoteResultReducer<String, String> {
		@Override
		public String reduce(List<AsterixRemoteResult<String>> result) {
			return result.get(0).getResult(); // Only one 'partition'
		}

	}
	
}
