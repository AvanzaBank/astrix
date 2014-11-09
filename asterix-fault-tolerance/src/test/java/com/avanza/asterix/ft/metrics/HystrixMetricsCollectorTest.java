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
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.avanza.asterix.ft.metrics.HystrixMetricsCollector;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommand.Setter;

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
		Map<String, Number> metrics = collector.getMetrics();
		Thread.sleep(1000); // TODO assertEventually
		assertThat(metrics.get(String.format("hystrix.${host}.%s.%s.isCircuitBreakerOpen", group, command)).intValue(), is(0));
		assertThat(metrics.get(String.format("hystrix.${host}.%s.currentQueueSize", group)).intValue(), is(0));
		assertThat(metrics.get(String.format("hystrix.${host}.%s.currentCompletedTaskCount", group)).intValue(), is(1));
	}

	private String randomString() {
		return "" + Math.random();
	}
	

}
