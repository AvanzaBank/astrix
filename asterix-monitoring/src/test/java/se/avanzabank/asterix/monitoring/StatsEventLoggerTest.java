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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import se.avanzabank.system.stats.Stats;

public class StatsEventLoggerTest {

	@Test
	public void increments() throws Exception {
		FakeStats stats = new FakeStats();
		StatsEventLogger eventLogger = new StatsEventLogger(stats);
		eventLogger.increment("foo.bar");
		assertThat(stats.incremented, contains("foo.bar"));
	}

	class FakeStats implements Stats {

		private Collection<String> incremented = new ArrayList<String>();

		@Override
		public void recordExecutionTime(String key, long executionTimeInMillis) {
		}

		@Override
		public void count(String key, int count) {
		}

		@Override
		public void increment(String key) {
			incremented.add(key);
		}

	}

}
