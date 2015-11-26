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
package com.avanza.astrix.context.metrics;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfigurations;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
import com.avanza.astrix.context.mbeans.AstrixMBeanExporter;
import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;

public class MetricsModule implements Module {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceBeanProxyFactory.class, ServiceBeanMetricsProxyFactory.class);
		moduleContext.bind(Metrics.class, MetricsImpl.class);
		
		moduleContext.importType(MetricsSpi.class);
		moduleContext.importType(AstrixConfig.class);
		moduleContext.importType(BeanConfigurations.class);
		moduleContext.importType(AstrixMBeanExporter.class);
		
		moduleContext.export(ServiceBeanProxyFactory.class);
		moduleContext.export(Metrics.class);
	}

}
