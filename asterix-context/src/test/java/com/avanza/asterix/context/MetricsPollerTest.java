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
package com.avanza.asterix.context;

import static org.hamcrest.Matchers.hasEntry;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import com.avanza.asterix.context.AsterixMetricsCollectorPlugin;
import com.avanza.asterix.context.AsterixMetricsLoggerPlugin;
import com.avanza.asterix.context.TestAsterixConfigurer;
import com.avanza.asterix.test.util.Poller;
import com.avanza.asterix.test.util.Probe;

public class MetricsPollerTest {

	@Test
	public void test() throws Exception {
		TestAsterixConfigurer configurer = new TestAsterixConfigurer();
		configurer.enableMonitoring(true);
		configurer.registerPlugin(AsterixMetricsCollectorPlugin.class, new FakeCollector());
		FakeLogger logger = new FakeLogger();
		configurer.registerPlugin(AsterixMetricsLoggerPlugin.class, logger);
		configurer.configure();
		assertEventually(loggedMetrics(logger, hasEntry("foo", (Number) 1)));
	}

	static class FakeCollector implements AsterixMetricsCollectorPlugin {

		@Override
		public Map<String, Number> getMetrics() {
			Map<String, Number> m = new HashMap<String, Number>();
			m.put("foo", 1);
			return m;
		}
	}

	static class FakeLogger implements AsterixMetricsLoggerPlugin {

		private Map<String, Number> lastLogged = new HashMap<>();
		
		@Override
		public void logMetrics(Map<String, Number> metrics) {
			lastLogged = metrics;
		}

	}
	
	public Probe loggedMetrics(final FakeLogger logger,
			final Matcher<Map<? extends String, ? extends Number>> matcher) {
		return new Probe() {

			Map<String, Number> metrics = new HashMap<String, Number>();

			@Override
			public void sample() {
				metrics = new HashMap<String, Number>(logger.lastLogged);
			}

			@Override
			public boolean isSatisfied() {
				return matcher.matches(metrics);
			}

			@Override
			public void describeFailureTo(Description description) {
				description.appendText("expected logged metrics ").appendDescriptionOf(matcher)
						.appendText(" but logged metrics were ").appendValue(metrics);
			}
		};
	}
	
	private void assertEventually(Probe probe) throws Exception {
		new Poller(1000L, 50L).check(probe);
	}

}
