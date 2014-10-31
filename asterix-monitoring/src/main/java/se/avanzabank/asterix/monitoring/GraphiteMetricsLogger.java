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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.context.AsterixMetricsLoggerPlugin;
import se.avanzabank.core.support.jndi.lookup.BasicJNDILookup;
import se.avanzabank.system.graphite.Graphite;
import se.avanzabank.system.graphite.GraphiteFactory;

/**
 * @author Kristoffer Erlandsson (krierl)
 */
@MetaInfServices(AsterixMetricsLoggerPlugin.class)
public class GraphiteMetricsLogger implements AsterixMetricsLoggerPlugin {

	private final GraphiteFacade graphite;
	private final Logger logger = LoggerFactory.getLogger(GraphiteMetricsLogger.class);

	public GraphiteMetricsLogger() {
		this.graphite = createGraphite();
	}

	private GraphiteFacade createGraphite() {
		try {
			return new GraphiteAdapter(new GraphiteFactory().getBatchGraphite(new BasicJNDILookup()));
		} catch (Exception e) {
			logger.warn("Failed to create Graphite instance. No data will be published to graphite", e);
			return new NullGraphite();
		}
	}
	
	public GraphiteMetricsLogger(Graphite graphite) {
		this.graphite = new GraphiteAdapter(Objects.requireNonNull(graphite));
	}

	@Override
	public void logMetrics(Map<String, Number> metrics) {
		for (Entry<String, Number> entry : metrics.entrySet()) {
			graphite.logHostMetric(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public String toString() {
		return "GraphiteMetricsLogger";
	}
	
	
}