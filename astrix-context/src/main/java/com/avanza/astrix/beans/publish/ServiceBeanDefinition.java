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
package com.avanza.astrix.beans.publish;

import java.util.Map;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDiscoveryDefinition;

public class ServiceBeanDefinition<T> {

	private final Map<BeanSetting<?>, Object> defaultBeanSettingsOverride;
	private final ServiceDefinition<T> serviceDefinition;
	private final ServiceDiscoveryDefinition serviceDiscoveryDefinition;
		
	public ServiceBeanDefinition(Map<BeanSetting<?>, Object> defaultBeanSettingsOverride,
			ServiceDefinition<T> serviceDefinition, ServiceDiscoveryDefinition serviceDiscoveryDefinition) {
		this.defaultBeanSettingsOverride = defaultBeanSettingsOverride;
		this.serviceDefinition = serviceDefinition;
		this.serviceDiscoveryDefinition = serviceDiscoveryDefinition;
	}

	public Map<BeanSetting<?>, Object> getDefaultBeanSettingsOverride() {
		return this.defaultBeanSettingsOverride;
	}

	public AstrixBeanKey<T> getBeanKey() {
		return this.serviceDefinition.getBeanKey();
	}
	
	public ServiceDefinition<T> getServiceDefinition() {
		return serviceDefinition;
	}
	
	public ServiceDiscoveryDefinition getServiceDiscoveryDefinition() {
		return serviceDiscoveryDefinition;
	}
	
}
