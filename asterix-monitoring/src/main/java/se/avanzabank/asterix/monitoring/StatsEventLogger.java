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

import java.util.Objects;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.context.AsterixEventLoggerPlugin;
import se.avanzabank.system.graphite.GraphiteFactory;
import se.avanzabank.system.stats.Stats;

/**
 * @author Kristoffer Erlandsson (krierl)
 */
@MetaInfServices(value = AsterixEventLoggerPlugin.class)
public class StatsEventLogger implements AsterixEventLoggerPlugin {

	private StatsFacade stats;
	private final static Logger logger = LoggerFactory.getLogger(StatsEventLogger.class);

	public StatsEventLogger() {
		this.stats = createStats();
	}
	
	public StatsEventLogger(Stats stats) {
		this.stats = new StatsAdapter(Objects.requireNonNull(stats));
	}

	private static StatsFacade createStats() {
		try {
			return new StatsAdapter(new GraphiteFactory().getStats());
		} catch (Exception e) {
			logger.warn("Failed to create Stats instance. No data will be published to graphite", e);
			return new NullStats();
		}
	}
	
	@Override
	public void increment(String event) {
		stats.increment(event);
	}

}
