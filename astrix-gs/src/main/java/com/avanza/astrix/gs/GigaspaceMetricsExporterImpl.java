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
package com.avanza.astrix.gs;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.mbeans.MBeanExporter;
import com.avanza.astrix.gs.metrics.GigaspaceMetrics;

final class GigaspaceMetricsExporterImpl implements AstrixConfigAware, GigaspaceMetricsExporter {
	private static final String MBEAN_FOLDER = "Gigaspaces";
	private static final String MBEAN_NAME = "Metrics";
	private final AtomicBoolean exported = new AtomicBoolean(false);
	private final MBeanExporter mBeanExporter;
	private DynamicBooleanProperty exportGigaspaceMetrics;

	GigaspaceMetricsExporterImpl(MBeanExporter mBeanExporter) {
		this.mBeanExporter = requireNonNull(mBeanExporter);
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.exportGigaspaceMetrics = AstrixSettings.EXPORTED_GIGASPACE_METRICS_ENABLED.getFrom(config);
		this.exportGigaspaceMetrics.addListener(this::toggleMBean);
	}

	@Override
	public void exportGigaspaceMetrics() {
		toggleMBean(exportGigaspaceMetrics.get());
	}

	private void toggleMBean(boolean enableMBean) {
		if (enableMBean) {
			if (exported.compareAndSet(false, true)) {
				mBeanExporter.registerMBean(new GigaspaceMetrics(), MBEAN_FOLDER, MBEAN_NAME);
			}
		} else {
			if (exported.compareAndSet(true, false)) {
				mBeanExporter.unregisterMBean(MBEAN_FOLDER, MBEAN_NAME);
			}
		}
	}
}
