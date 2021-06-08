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

import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.context.metrics.MetricsSpi;
import com.avanza.astrix.context.metrics.TimerSnaphot;
import com.avanza.astrix.context.metrics.TimerSpi;
import com.avanza.astrix.core.function.CheckedCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rx.Observable;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DropwizardMetricsTest {

	
	private DropwizardMetrics dropwizardMetrics;
	private AstrixApplicationContext astrixContext;

	@BeforeEach
	void setup() {
		astrixContext = (AstrixApplicationContext) new TestAstrixConfigurer().configure();
		MetricsSpi metricsSpi = astrixContext.getInstance(MetricsSpi.class);
		
		assertEquals(DropwizardMetrics.class, metricsSpi.getClass());
		
		dropwizardMetrics = (DropwizardMetrics) metricsSpi;
	}
	
	@AfterEach
	void cleanup() throws Exception {
		astrixContext.close();
	}
	
	@Test
	void timeExecution() throws Throwable {

		TimerSpi timer = dropwizardMetrics.createTimer();
		
		CheckedCommand<String> execution = timer.timeExecution(() -> {
			Thread.sleep(10);
			return "foo-bar";
		});
		
		assertEquals("foo-bar", execution.call());
		
		// Should meassure execution time roughly equal to 10 ms
		TimerSnaphot timerSnapshot = timer.getSnapshot();
		assertEquals(1, timerSnapshot.getCount());
		assertThat(timerSnapshot.getMax(), greaterThan(8D));
	}
	

	@Test
	void timeObservable() {
		TimerSpi timer = dropwizardMetrics.createTimer();
		
		Supplier<Observable<String>> observable = timer.timeObservable(() -> Observable.unsafeCreate(subscriber -> {
			try {
				Thread.sleep(10);
				subscriber.onNext("foo");
				subscriber.onCompleted();
			} catch (InterruptedException e) {
				subscriber.onError(e);
			}
		}));
		
		assertEquals("foo", observable.get().toBlocking().first());

		TimerSnaphot timerSnapshot = timer.getSnapshot();
		assertEquals(1, timerSnapshot.getCount());
		// Should measure execution time roughly equal to 10 ms
		assertThat(timerSnapshot.getMax(), greaterThan(8D));
	}

}
