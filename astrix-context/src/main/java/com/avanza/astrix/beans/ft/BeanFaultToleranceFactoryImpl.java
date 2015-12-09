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
package com.avanza.astrix.beans.ft;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.context.mbeans.MBeanExporter;
/**
 * 
 * @author Elias Lindholm
 *
 */
final class BeanFaultToleranceFactoryImpl implements BeanFaultToleranceFactory {
	
	
	private final BeanFaultToleranceFactorySpi beanFaultToleranceFactorySpi;
	private final AstrixConfig config;
	private final MBeanExporter mbeanExporter;
	
	public BeanFaultToleranceFactoryImpl(BeanFaultToleranceFactorySpi beanFaultToleranceFactorySpi,
									     AstrixConfig config,
									     MBeanExporter mbeanExporter) {
		this.beanFaultToleranceFactorySpi = beanFaultToleranceFactorySpi;
		this.config = config;
		this.mbeanExporter = mbeanExporter;
	}

	@Override
	public BeanProxy createFaultToleranceProxy(AstrixBeanKey<?> beanKey) {
		BeanConfiguration beanConfiguration = config.getBeanConfiguration(beanKey);
		BeanFaultTolerance beanFaultTolerance = beanFaultToleranceFactorySpi.create(beanKey);
		BeanFaultToleranceProxy result = new BeanFaultToleranceProxy(beanConfiguration, config.getConfig(), beanFaultTolerance);
		if (beanFaultToleranceFactorySpi instanceof MonitorableFaultToleranceSpi) {
			Object mbean = MonitorableFaultToleranceSpi.class.cast(beanFaultToleranceFactorySpi).createBeanFaultToleranceMetricsMBean(beanKey);
			mbeanExporter.registerMBean(mbean, "BeanFaultToleranceMetrics", beanKey.toString());
		}
		return result;
	}
	
}
