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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.InvocationExecutionWatcher;
import com.avanza.astrix.beans.tracing.InvocationExecutionWatcher.AfterInvocation;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;

import rx.Observable;

public class RemotingTracingTest {

	private Ping ping;
	private PingFuture pingFuture;
	private PingObservable pingObservable;
	private InvocationExecutionWatcher clientInvocationWatcher = mock(InvocationExecutionWatcher.class);
	private InvocationExecutionWatcher serverInvocationWatcher = mock(InvocationExecutionWatcher.class);
	private AfterInvocation afterClientInvocationWatcher = mock(AfterInvocation.class);
	private AfterInvocation afterServerInvocationWatcher = mock(AfterInvocation.class);
	private AstrixTraceProvider mockTraceProvider = mock(AstrixTraceProvider.class);
	private String correlationId = UUID.randomUUID().toString();

	@Before
	public void beforeEachTest() {
		when(clientInvocationWatcher.beforeInvocation(any())).thenReturn(afterClientInvocationWatcher);
		when(serverInvocationWatcher.beforeInvocation(any())).thenReturn(afterServerInvocationWatcher);
		when(mockTraceProvider.getClientCallExecutionWatchers(any(), any()))
				.thenReturn(Collections.singletonList(clientInvocationWatcher));
		when(mockTraceProvider.getServerCallExecutionWatchers(any(), any()))
				.thenReturn(Collections.singletonList(serverInvocationWatcher));
		when(mockTraceProvider.getCorrelationId(any()))
				.thenReturn(correlationId);

		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver(mockTraceProvider);
		remotingDriver.registerServer(Ping.class, new PingImpl());
		ping = remotingDriver.createRemotingProxy(Ping.class);
		pingFuture = remotingDriver.createRemotingProxy(PingFuture.class, Ping.class);
		pingObservable = remotingDriver.createRemotingProxy(PingObservable.class, Ping.class);
	}

	@Test
	public void shouldCallInvocationWatchers() {
		String response = ping.ping("msg");

		assertThat(response, equalTo("msg"));
		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldCallInvocationWatchersFuture() throws Exception {
		String response = pingFuture.ping("msg").get();

		assertThat(response, equalTo("msg"));
		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldCallInvocationWatchersObservable() {
		String response = pingObservable.ping("msg").toBlocking().single();

		assertThat(response, equalTo("msg"));
		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldNotCallAllInvocationWatchersIfObservableIsNotCompleted() {
		pingObservable.ping("msg");

		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher, never()).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldCallInvocationWatchersOnException() {
		try {
			ping.pingThrows("msg");
			fail("Expected an exception to be thrown here, but no exception was seen");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(correlationId));
		}

		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldCallInvocationWatchersFutureOnException() {
		try {
			pingFuture.pingThrows("msg").get();
			fail("Expected an exception to be thrown here, but no exception was seen");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(correlationId));
		}

		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	@Test
	public void shouldCallInvocationWatchersObservableOnException() {
		String response = pingObservable.pingThrows("msg")
					.onErrorReturn(e -> {
						assertThat(e.getMessage(), containsString(correlationId));
						return "thrown";
					})
					.toBlocking()
					.single();

		assertThat(response, equalTo("thrown"));
		verify(clientInvocationWatcher).beforeInvocation(any());
		verify(afterClientInvocationWatcher).afterInvocation();
		verify(serverInvocationWatcher).beforeInvocation(any());
		verify(afterServerInvocationWatcher).afterInvocation();
	}

	private static final class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}

		@Override
		public String pingThrows(String msg) {
			throw new RuntimeException(msg);
		}
	}

	public interface Ping {
		String ping(String msg);
		String pingThrows(String msg);
	}

	public interface PingFuture {
		Future<String> ping(String msg);
		Future<String> pingThrows(String msg);
	}

	public interface PingObservable {
		Observable<String> ping(String msg);
		Observable<String> pingThrows(String msg);
	}

	@AstrixApiProvider
	public interface PingApi {
		@AstrixConfigDiscovery("ping")
		@Service
		Ping ping();
	}
}
