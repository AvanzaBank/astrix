package com.avanza.astrix.gs;

import static com.avanza.astrix.beans.core.AstrixSettings.EXPORTED_GIGASPACE_METRICS_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.mbeans.MBeanExporter;
import com.avanza.astrix.gs.metrics.GigaspaceMetrics;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceMetricsExporterImplTest {
	private static final String MBEAN_FOLDER = "Gigaspaces";
	private static final String MBEAN_NAME = "Metrics";

	@Mock
	private MBeanExporter mBeanExporter;

	private final MapConfigSource configSource = new MapConfigSource();

	private GigaspaceMetricsExporterImpl target;

	@Before
	public void setUp() {

		target = new GigaspaceMetricsExporterImpl(mBeanExporter);
		target.setConfig(new DynamicConfig(configSource));
	}

	@Test
	public void shouldExportGigaspaceMetricsMBeanWhenCalled() {
		target.exportGigaspaceMetrics();

		verify(mBeanExporter).registerMBean(isA(GigaspaceMetrics.class), eq(MBEAN_FOLDER), eq(MBEAN_NAME));
	}

	@Test
	public void shouldNotExportGigaspaceMetricsMBeanWhenDisabled() {
		configSource.set(EXPORTED_GIGASPACE_METRICS_ENABLED, false);

		target.exportGigaspaceMetrics();

		verify(mBeanExporter, never()).registerMBean(any(GigaspaceMetrics.class), anyString(), anyString());
	}

	@Test
	public void shouldExportGigaspaceMetricsWhenToggledOn() {
		configSource.set(EXPORTED_GIGASPACE_METRICS_ENABLED, false);
		configSource.set(EXPORTED_GIGASPACE_METRICS_ENABLED, true);

		verify(mBeanExporter).registerMBean(isA(GigaspaceMetrics.class), eq(MBEAN_FOLDER), eq(MBEAN_NAME));
	}

	@Test
	public void shouldUnregisterMBeanWhenToggledOff() {
		target.exportGigaspaceMetrics();

		configSource.set(EXPORTED_GIGASPACE_METRICS_ENABLED, false);

		verify(mBeanExporter).unregisterMBean(MBEAN_FOLDER, MBEAN_NAME);
	}

}