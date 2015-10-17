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
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.publish.SimplePublishedAstrixBean;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;

public class ServiceBeanMetricsProxyFactory implements ServiceBeanProxyFactory {

	private final MetricsSpi metrics;
	private final AstrixConfig astrixConfig;
	private final BeanConfigurations beanConfigurations;
	
	public ServiceBeanMetricsProxyFactory(MetricsSpi metrics, AstrixConfig astrixConfig, BeanConfigurations beanConfigurations) {
		this.metrics = metrics;
		this.astrixConfig = astrixConfig;
		this.beanConfigurations = beanConfigurations;
	}

	@Override
	public BeanProxy create(ServiceDefinition<?> serviceDefinition, ServiceComponent serviceComponent) {
		return new BeanMetricsProxy(SimplePublishedAstrixBean.from(serviceDefinition), metrics, astrixConfig, beanConfigurations);
	}

	@Override
	public int order() {
		return 2;
	}

}
