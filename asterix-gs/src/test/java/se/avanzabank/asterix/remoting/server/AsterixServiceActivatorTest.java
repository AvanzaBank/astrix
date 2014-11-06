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
package se.avanzabank.asterix.remoting.server;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.openspaces.remoting.Routing;

import rx.Observable;
import se.avanzabank.asterix.core.AsterixBroadcast;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.core.AsterixRemoteResult;
import se.avanzabank.asterix.core.AsterixRemoteResultReducer;
import se.avanzabank.asterix.remoting.client.AsterixMissingServiceException;
import se.avanzabank.asterix.remoting.client.AsterixRemoteServiceException;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;

import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceActivatorTest {
	
	AsterixObjectSerializer objectSerializer = new AsterixObjectSerializer.NoVersioningSupport();
	AsterixServiceActivator activator = new AsterixServiceActivator();
	
	@Test
	public void invokesAServiceThrougTransportAndServiceActivator() throws Exception {
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
			public <T> T deserialize(Object element, Class<T> target, int version) {
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
	
	public static class HelloRequest {
		private String messsage;
		
		public HelloRequest(String messsage) {
			this.messsage = messsage;
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
		
		public String getMesssage() {
			return messsage;
		}

		public void setMesssage(String messsage) {
			this.messsage = messsage;
		}
	}
	
	public static class HelloResponse {
		private String greeting;
		
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
		String hello();
	}
	
	interface TestService {
		HelloResponse hello(HelloRequest message);
		String hello(HelloRequest message, String greeting);
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
