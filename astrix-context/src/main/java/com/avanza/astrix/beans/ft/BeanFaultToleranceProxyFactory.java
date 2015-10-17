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
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.ft.FaultToleranceConfigurator.FtProxySetting;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.beans.publish.SimplePublishedAstrixBean;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.context.core.BeanProxy;
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
	private final HystrixCommandNamingStrategy commandNamingStrategy;
	
	public BeanFaultToleranceProxyFactory(FaultToleranceSpi beanFaultToleranceSpi,
									      BeanConfigurations beanConfigurations, 
									      AstrixConfig config,
									      HystrixCommandNamingStrategy commandNamingStrategy) {
		this.beanFaultToleranceSpi = beanFaultToleranceSpi;
		this.beanConfigurations = beanConfigurations;
		this.config = config;
		this.commandNamingStrategy = commandNamingStrategy;
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
		PublishedAstrixBean<?> publishedBeanInfo = SimplePublishedAstrixBean.from(serviceDefinition);
		CommandSettings ftSettings = createCommandSettingsSettings(beanConfiguration, publishedBeanInfo);
		return new BeanFaultToleranceProxy(beanConfiguration, config.getConfig(), beanFaultToleranceSpi, ftSettings);
	}

	@Override
	public BeanProxy createFaultToleranceProxy(PublishedAstrixBean<?> serviceDefinition) {
		BeanConfiguration beanConfiguration = beanConfigurations.getBeanConfiguration(serviceDefinition.getBeanKey());
		return new BeanFaultToleranceProxy(beanConfiguration, config.getConfig(), 
				beanFaultToleranceSpi, createCommandSettingsSettings(beanConfiguration, serviceDefinition));
	}

	private CommandSettings createCommandSettingsSettings(BeanConfiguration beanConfiguration, PublishedAstrixBean<?> publishedBeanInfo) {
		CommandSettings ftSettings = new CommandSettings();
		ftSettings.setCommandName(commandNamingStrategy.getCommandKeyName(publishedBeanInfo));
		ftSettings.setGroupName(commandNamingStrategy.getGroupKeyName(publishedBeanInfo));
		ftSettings.setInitialTimeoutInMilliseconds(beanConfiguration.get(AstrixBeanSettings.INITIAL_TIMEOUT).get());
		ftSettings.setInitialSemaphoreMaxConcurrentRequests(beanConfiguration.get(AstrixBeanSettings.INITIAL_MAX_CONCURRENT_REQUESTS).get());
		ftSettings.setInitialCoreSize(beanConfiguration.get(AstrixBeanSettings.INITIAL_CORE_SIZE).get());
		ftSettings.setInitialQueueSizeRejectionThreshold(beanConfiguration.get(AstrixBeanSettings.INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD).get());
		return ftSettings;
	}
	
	@Override
	public int order() {
		return 1;
	}
	
}
