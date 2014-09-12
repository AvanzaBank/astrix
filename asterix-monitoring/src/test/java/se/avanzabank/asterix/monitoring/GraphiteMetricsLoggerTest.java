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
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import se.avanzabank.system.graphite.Graphite;

public class GraphiteMetricsLoggerTest {

	@SuppressWarnings("serial")
	@Test
	public void test() {
		FakeGraphite fakeGraphite = new FakeGraphite();
		GraphiteMetricsLogger logger = new GraphiteMetricsLogger(fakeGraphite);
		logger.logMetrics(new HashMap<String, Number>() {{ put("foo", 2); }});
		assertThat(fakeGraphite.loggedHostMetrics.get("foo"), is((Number)2));
	}

	
	static class FakeGraphite implements Graphite {
		Map<String, Number> loggedHostMetrics = new HashMap<String, Number>();

		@Override
		public void logMetric(String key, Number value) {
			
		}

		@Override
		public void logHostMetric(String keyFormat, Number value) {
			loggedHostMetrics.put(keyFormat, value);
		}

		@Override
		public void logEvent(String key) {
			
		}

		@Override
		public void logHostEvent(String keyFormat) {
			
		}
	}
}
