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
package com.avanza.astrix.ft;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class BeanFaultToleranceFactoryImpl implements BeanFaultToleranceFactory {
	
	private final BeanConfigurations beanConfigurations;
	private final AstrixConfig astrixConfig;
	private final FaultToleranceSpi faultTolerance;
	private final HystrixCommandNamingStrategy commandNamingStrategy;
	
	public BeanFaultToleranceFactoryImpl(BeanConfigurations beanConfigurations, AstrixConfig astrixConfig, FaultToleranceSpi faultTolerance, HystrixCommandNamingStrategy commandNamingStrategy) {
		this.beanConfigurations = beanConfigurations;
		this.astrixConfig = astrixConfig;
		this.faultTolerance = faultTolerance;
		this.commandNamingStrategy = commandNamingStrategy;
	}

	public <T> T addFaultToleranceProxy(PublishedAstrixBean<T> serviceDefinition, T target, CommandSettings commandSettings) {
		return create(serviceDefinition, commandSettings).addFaultToleranceProxy(serviceDefinition.getBeanKey().getBeanType(), target);
	}

	public BeanFaultToleranceImpl create(PublishedAstrixBean<?> serviceDefinition, CommandSettings commandSettings) {
		BeanConfiguration beanConfiguration = beanConfigurations.getBeanConfiguration(serviceDefinition.getBeanKey());
		commandSettings.setCommandName(commandNamingStrategy.getCommandKeyName(serviceDefinition));
		commandSettings.setGroupName(commandNamingStrategy.getGroupKeyName(serviceDefinition));
		commandSettings.setInitialTimeoutInMilliseconds(beanConfiguration.get(AstrixBeanSettings.INITIAL_TIMEOUT).get());
		return new BeanFaultToleranceImpl(beanConfiguration, astrixConfig.getConfig(), faultTolerance, commandSettings);
	}
	
	
}
