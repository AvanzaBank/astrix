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
package com.avanza.astrix.beans.service;

import java.util.Collections;
import java.util.Map;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanSettings;

public class AstrixServiceBeanInstance implements AstrixServiceBeanInstanceMBean {
	
	private BeanConfiguration beanConfiguration;
	private AstrixConfig astrixConfig;
	private ServiceBeanInstance<?> instance;
	
	public AstrixServiceBeanInstance(BeanConfiguration beanConfiguration, AstrixConfig astrixConfig, ServiceBeanInstance<?> serviceBeanInstance) {
		this.beanConfiguration = beanConfiguration;
		this.astrixConfig = astrixConfig;
		this.instance = serviceBeanInstance;
	}

	@Override
	public boolean isAvailable() {
		return beanConfiguration.get(AstrixBeanSettings.AVAILABLE).get();
	}
	
	@Override
	public boolean isFaultToleranceEnabled() {
		return beanConfiguration.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED).get();
	}
	
	@Override
	public void setAvailable(boolean available) {
		astrixConfig.set(AstrixBeanSettings.AVAILABLE.nameFor(beanConfiguration.getBeanKey()), Boolean.toString(available));
	}
	
	@Override
	public String getState() {
		return instance.getState();
	}
	
	@Override
	public Map<String, String> getServiceProperties() {
		ServiceProperties currentProperties = this.instance.getCurrentProperties();
		if (currentProperties == null) {
			return Collections.emptyMap();
		}
		return currentProperties.getProperties();
	}
	
	
}