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

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.config.BeanConfigurations;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.ft.FaultToleranceConfigurator.FtProxySetting;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
/**
 * 
 * @author Elias Lindholm
 *
 */
final class BeanFaultToleranceProxyFactory implements ServiceBeanProxyFactory, BeanFaultToleranceFactory {
	
	private static final Logger log = LoggerFactory.getLogger(BeanFaultToleranceProxyFactory.class);

	private final FaultToleranceSpi beanFaultToleranceSpi;
	private final BeanConfigurations beanConfigurations;
	private final AstrixConfig config;
	
	public BeanFaultToleranceProxyFactory(FaultToleranceSpi beanFaultToleranceSpi,
									      BeanConfigurations beanConfigurations, 
									      AstrixConfig config) {
		this.beanFaultToleranceSpi = beanFaultToleranceSpi;
		this.beanConfigurations = beanConfigurations;
		this.config = config;
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
		BeanConfiguration beanConfiguration = beanConfigurations.getBeanConfiguration(serviceDefinition.getBeanKey());
		return new BeanFaultToleranceProxy(beanConfiguration, config.getConfig(), beanFaultToleranceSpi);
	}

	@Override
	public BeanProxy createFaultToleranceProxy(PublishedAstrixBean<?> serviceDefinition) {
		BeanConfiguration beanConfiguration = beanConfigurations.getBeanConfiguration(serviceDefinition.getBeanKey());
		return new BeanFaultToleranceProxy(beanConfiguration, config.getConfig(), 
				beanFaultToleranceSpi);
	}
	
	@Override
	public int order() {
		return 1;
	}
	
}
