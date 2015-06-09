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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.publish.AstrixBeanDefinition;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanFaultToleranceFactory implements AstrixConfigAware {
	
	private final Logger log = LoggerFactory.getLogger(BeanFaultToleranceFactory.class);
	private DynamicConfig config;
	private BeanFaultToleranceProvider faultToleranceProvider;
	private HystrixCommandNamingStrategy commandNamingStrategy;
	
	@AstrixInject
	public BeanFaultToleranceFactory(BeanFaultToleranceProvider beanFaultToleranceProvider) {
		this.faultToleranceProvider = beanFaultToleranceProvider;
	}
	
	public BeanFaultToleranceFactory(DynamicConfig config, BeanFaultToleranceProvider faultToleranceProvider, HystrixCommandNamingStrategy commandNamingStrategy) {
		this.faultToleranceProvider = faultToleranceProvider;
		this.config = config;
		this.commandNamingStrategy = commandNamingStrategy;
	}

	public BeanFaultTolerance create(AstrixBeanDefinition<?> serviceDefinition) {
		return new BeanFaultTolerance(serviceDefinition, config, faultToleranceProvider, commandNamingStrategy);
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
		this.commandNamingStrategy = createCommandNamingStragety(config);
	}
	
	private HystrixCommandNamingStrategy createCommandNamingStragety(DynamicConfig config) {
		String commandNamingStrategy = config.getStringProperty(HystrixCommandNamingStrategy.class.getName()
				, HystrixCommandNamingStrategy.Default.class.getName()).get();
		log.info("Using HystrixCommandNamingStrategy: {}", HystrixCommandNamingStrategy.class.getName());
		return (HystrixCommandNamingStrategy) ReflectionUtil.newInstance(ReflectionUtil.classForName(commandNamingStrategy));
	}
	
	public HystrixCommandNamingStrategy getCommandNamingStrategy() {
		return commandNamingStrategy;
	}
}
