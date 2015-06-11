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

import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.publish.AstrixBeanDefinition;
import com.avanza.astrix.config.DynamicConfig;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanFaultToleranceFactory implements AstrixConfigAware {
	
	private DynamicConfig config;
	private final BeanFaultToleranceProvider faultToleranceProvider;
	private final HystrixCommandNamingStrategy commandNamingStrategy;
	private final BeanConfigurations beanConfigurations;
	
	@AstrixInject
	public BeanFaultToleranceFactory(BeanFaultToleranceProvider beanFaultToleranceProvider, HystrixCommandNamingStrategy commandNamingStrategy, BeanConfigurations beanConfigurations) {
		this.faultToleranceProvider = beanFaultToleranceProvider;
		this.commandNamingStrategy = commandNamingStrategy;
		this.beanConfigurations = beanConfigurations;
	}
	
	public BeanFaultToleranceFactory(DynamicConfig config, BeanFaultToleranceProvider faultToleranceProvider, HystrixCommandNamingStrategy commandNamingStrategy, BeanConfigurations beanConfigurations) {
		this.faultToleranceProvider = faultToleranceProvider;
		this.config = config;
		this.commandNamingStrategy = commandNamingStrategy;
		this.beanConfigurations = beanConfigurations;
	}

	public BeanFaultTolerance create(AstrixBeanDefinition<?> serviceDefinition) {
		return new BeanFaultTolerance(serviceDefinition, beanConfigurations.getBeanConfiguration(serviceDefinition.getBeanKey()), config, faultToleranceProvider, commandNamingStrategy);
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	public HystrixCommandNamingStrategy getCommandNamingStrategy() {
		return commandNamingStrategy;
	}
}
