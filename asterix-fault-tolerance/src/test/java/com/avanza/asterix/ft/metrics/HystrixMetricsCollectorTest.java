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
package com.avanza.asterix.ft.metrics;

import static org.hamcrest.Matchers.*;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import com.avanza.asterix.test.util.Poller;
import com.avanza.asterix.test.util.Probe;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

public class HystrixMetricsCollectorTest {

	@Test
	public void getMetrics() throws Exception {
		HystrixMetricsCollector collector = new HystrixMetricsCollector();
		String group = randomString();
		String command = randomString();
		Setter commandConfig = Setter
				.withGroupKey(HystrixCommandGroupKey.Factory.asKey(group))
				.andCommandKey(HystrixCommandKey.Factory.asKey(command));
		new HystrixCommand<Integer>(commandConfig) {

			@Override
			protected Integer run() throws Exception {
				return 1;
			}

		}.execute();
		assertEventually(metricHasValue(collector, String.format("hystrix.${host}.%s.%s.isCircuitBreakerOpen", group, command), is(0L)));
		assertEventually(metricHasValue(collector, String.format("hystrix.${host}.%s.currentQueueSize", group), is(0L)));
		assertEventually(metricHasValue(collector, String.format("hystrix.${host}.%s.currentCompletedTaskCount", group), is(1L)));
	}

	private Probe metricHasValue(final HystrixMetricsCollector collector, final String format, final Matcher<? extends Number> matcher) {
		return new Probe() {
			
			private Map<String, Number> metrics;

			@Override
			public void sample() {
				metrics = collector.getMetrics();
			}
			
			@Override
			public boolean isSatisfied() {
				return matcher.matches(metrics.get(format));
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("expected metrics").appendDescriptionOf(matcher).appendText("was").appendValue(metrics.get(format));
			}
		};
	}

	private String randomString() {
		return "" + Math.random();
	}
	
	private void assertEventually(Probe p) throws Exception {
		new Poller(3000, 50).check(p);
	}
	

}
