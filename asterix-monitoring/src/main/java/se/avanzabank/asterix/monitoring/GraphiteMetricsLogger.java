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

import se.avanzabank.asterix.context.AsterixMetricsLoggerPlugin;
import se.avanzabank.system.graphite.Graphite;

@MetaInfServices(AsterixMetricsLoggerPlugin.class)
public class GraphiteMetricsLogger implements AsterixMetricsLoggerPlugin {

	private Graphite graphite;

	public GraphiteMetricsLogger() {
		
	}
	
	public GraphiteMetricsLogger(Graphite graphite) {
		this.graphite = Objects.requireNonNull(graphite);
	}

	@Override
	public void logMetrics(Map<String, Number> metrics) {
		for (Entry<String, Number> entry : metrics.entrySet()) {
			graphite.logHostMetric(entry.getKey(), entry.getValue());
		}
	}

}
