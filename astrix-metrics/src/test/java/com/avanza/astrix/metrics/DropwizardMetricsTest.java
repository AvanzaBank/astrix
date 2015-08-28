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
package com.avanza.astrix.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.context.metrics.MetricsSpi;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.function.Supplier;
import com.codahale.metrics.Timer;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public class DropwizardMetricsTest {

	
	private DropwizardMetrics dropwizardMetrics;
	private AstrixApplicationContext astrixContext;

	@Before
	public void setup() {
		astrixContext = (AstrixApplicationContext) new TestAstrixConfigurer().configure();
		MetricsSpi metricsSpi = astrixContext.getInstance(MetricsSpi.class);
		
		assertEquals(DropwizardMetrics.class, metricsSpi.getClass());
		
		dropwizardMetrics = (DropwizardMetrics) metricsSpi;
	}
	
	@After
	public void cleanup() throws Exception {
		astrixContext.close();
	}
	
	@Test
	public void timeExecution() throws Throwable {
		
		CheckedCommand<String> execution = dropwizardMetrics.timeExecution(new CheckedCommand<String>() {
			@Override
			public String call() throws Throwable {
				Thread.sleep(2);
				return "foo";
			}
		}, "bar-group", "foo-metrics");
		
		assertEquals("foo", execution.call());
		
		Timer fooMetrics = dropwizardMetrics.getMetrics().getTimers().get("bar-group#foo-metrics");
		assertEquals(1, fooMetrics.getCount());
		// Should meassure execution time roughly equal to 2000 us
		assertTrue(fooMetrics.getSnapshot().getMean() > 100_000);
	}
	
	@Test
	public void timeObservable() throws Throwable {
		
		Supplier<Observable<String>> observable = dropwizardMetrics.timeObservable(new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.create(new OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> t) {
						try {
							Thread.sleep(2);
							t.onNext("foo");
							t.onCompleted();
						} catch (InterruptedException e) {
							t.onError(e);
						}
					}
					
				});
			}
		}, "bar-group", "foo-metrics");
		
		assertEquals("foo", observable.get().toBlocking().first());
		
		Timer fooMetrics = dropwizardMetrics.getMetrics().getTimers().get("bar-group#foo-metrics");
		assertEquals(1, fooMetrics.getCount());
		// Should meassure execution time roughly equal to 2000 us
		assertTrue(fooMetrics.getSnapshot().getMean() > 100_000);
	}

}
