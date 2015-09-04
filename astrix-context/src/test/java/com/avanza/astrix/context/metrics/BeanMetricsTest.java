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
package com.avanza.astrix.context.metrics;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BasicFuture;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

public class BeanMetricsTest {
	
	private AstrixContext astrixContext;

	@After
	public void after() {
		AstrixTestUtil.closeSafe(astrixContext);
	}
	
	@Test
	public void timesAsyncInvocationsUsingTimeObservable() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.observePing("foo").toBlocking().first());
		assertEquals(2, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	@Test
	public void timesAsyncInvocationsWithFutureReturnType() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(2, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	@Test
	public void itsPossibleToDisableBeanMetrics() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixBeanSettings.BEAN_METRICS_ENABLED.nameFor(AstrixBeanKey.create(Ping.class)), false);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	@Test
	public void itsPossibleToDisableBeanMetricsGlobally() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixSettings.ENABLE_BEAN_METRICS, false);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	
	private static final class TwoClockTickPing implements Ping {
		private final AtomicLong fakeClock;

		private TwoClockTickPing(AtomicLong fakeClock) {
			this.fakeClock = fakeClock;
		}

		@Override
		public Observable<String> observePing(final String msg) {
			return Observable.create(new OnSubscribe<String>() {
				@Override
				public void call(Subscriber<? super String> t) {
					fakeClock.incrementAndGet(); // Simulate 2 ms execution time by ticking clock
					fakeClock.incrementAndGet();
					t.onNext(msg);
					t.onCompleted();
				}
			});
		}

		@Override
		public Future<String> pingAsync(String msg) {
			return new BasicFuture<>(ping(msg));
		}
		
		@Override
		public String ping(String msg) {
			fakeClock.incrementAndGet();
			fakeClock.incrementAndGet();
			return msg;
		}
	}

	private static final class FakeTimer implements MetricsSpi {
		private final AtomicLong fakeClock;
		private long lastTimedObservableTime = -1L;

		private FakeTimer(AtomicLong fakeClock) {
			this.fakeClock = fakeClock;
		}
		
		public long getLastTimedObservableExecutionTime() {
			return this.lastTimedObservableTime;
		}

		@Override
		public <T> Supplier<Observable<T>> timeObservable(final Supplier<Observable<T>> targetObservable, String group, String name) {
			return new Supplier<Observable<T>>() {
				@Override
				public Observable<T> get() {
					final long start = fakeClock.get(); // Set time before execution
					return targetObservable.get().map(new Func1<T, T>() {
						@Override
						public T call(T t) {
							lastTimedObservableTime = fakeClock.get() - start;
							return t;
						}
					});
				}
			};
		}

		@Override
		public <T> CheckedCommand<T> timeExecution(final CheckedCommand<T> execution, final String group, final String name) {
			return new CheckedCommand<T>() {
				@Override
				public T call() throws Throwable {
					return execution.call();
				}
			};
		}
	}

	public interface Ping {
		Observable<String> observePing(String msg);
		Future<String> pingAsync(String msg);
		String ping(String msg);
	}
	
	@AstrixApiProvider
	public interface PingApi {
		@AstrixConfigDiscovery("ping")
		@Service
		Ping ping();
	}

}
