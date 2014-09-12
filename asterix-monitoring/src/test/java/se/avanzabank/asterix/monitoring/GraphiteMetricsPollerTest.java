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
package se.avanzabank.asterix.monitoring;

import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import se.avanzabank.asterix.context.AsterixMetricsCollectorPlugin;
import se.avanzabank.asterix.context.AsterixMetricsLoggerPlugin;
import se.avanzabank.core.test.util.async.Poller;
import se.avanzabank.core.test.util.async.Probe;

public class GraphiteMetricsPollerTest {

	@Test
	public void pollsAndSends() throws Exception {
		AsterixMetricsCollectorPlugin collectorPlugin = new FakeCollectorPlugin();
		FakeLoggerPlugin loggerPlugin = new FakeLoggerPlugin();
		GraphiteMetricsPoller poller = new GraphiteMetricsPoller();
		poller.setMetricsCollectors(Arrays.asList(collectorPlugin));
		poller.setMetricsLogger(loggerPlugin);
		poller.start();
		assertEventually(loggedMetrics(loggerPlugin, hasEntry("foo", (Number) 1)));
	}

	private void assertEventually(Probe probe) throws Exception {
		new Poller(1000L, 50L).check(probe);
	}

	class FakeCollectorPlugin implements AsterixMetricsCollectorPlugin {

		@Override
		public Map<String, Number> getMetrics() {
			HashMap<String, Number> m = new HashMap<String, Number>();
			m.put("foo", 1);
			return m;
		}

	}

	class FakeLoggerPlugin implements AsterixMetricsLoggerPlugin {

		Map<String, Number> loggedMetrics = new HashMap<String, Number>();

		@Override
		public void logMetrics(Map<String, Number> metrics) {
			loggedMetrics.putAll(metrics);
		}

	}

	public Probe loggedMetrics(final FakeLoggerPlugin logger,
			final Matcher<Map<? extends String, ? extends Number>> matcher) {
		return new Probe() {

			Map<String, Number> metrics = new HashMap<String, Number>();

			@Override
			public void sample() {
				metrics = new HashMap<String, Number>(logger.loggedMetrics);
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

}
