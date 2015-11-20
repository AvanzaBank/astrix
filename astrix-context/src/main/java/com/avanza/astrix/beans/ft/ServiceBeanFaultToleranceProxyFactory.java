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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.ft.FaultToleranceConfigurator.FtProxySetting;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
/**
 * 
 * @author Elias Lindholm
 *
 */
final class ServiceBeanFaultToleranceProxyFactory implements ServiceBeanProxyFactory {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceBeanFaultToleranceProxyFactory.class);

	private final BeanFaultToleranceFactory ftFactory;
	
	public ServiceBeanFaultToleranceProxyFactory(BeanFaultToleranceFactory ftFactory) {
		this.ftFactory = ftFactory;
	}

	@Override
	public BeanProxy create(ServiceDefinition<?> serviceDefinition, ServiceComponent serviceComponent) {
		FtProxySetting ftProxySetting = FtProxySetting.ENABLED;
		if (serviceComponent instanceof FaultToleranceConfigurator) {
			 ftProxySetting = FaultToleranceConfigurator.class.cast(serviceComponent).configure();
		}
		if (ftProxySetting != FtProxySetting.ENABLED) {
			log.info("Fault tolerance proxy is disabled by ServiceComponent. componentName={}, beanKey={}", 
					serviceComponent.getName(), serviceDefinition.getBeanKey().toString());
			return BeanProxy.NoProxy.create();
		}
		return ftFactory.createFaultToleranceProxy(serviceDefinition.getBeanKey());
	}
	
	@Override
	public int order() {
		return 1;
	}
	
}
