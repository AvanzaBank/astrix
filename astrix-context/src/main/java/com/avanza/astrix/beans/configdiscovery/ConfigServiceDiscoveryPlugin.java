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
package com.avanza.astrix.beans.configdiscovery;

import java.util.Arrays;
import java.util.Collections;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.beans.service.ServiceDiscovery;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactoryPlugin;
import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
import com.avanza.astrix.beans.service.ServiceProviders;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ConfigServiceDiscoveryPlugin implements ServiceDiscoveryFactoryPlugin<ConfigDiscoveryProperties> {

	private final ServiceComponentRegistry serviceComponents;
	private final AstrixConfig config;

	public ConfigServiceDiscoveryPlugin(ServiceComponentRegistry serviceComponents, AstrixConfig config) {
		this.serviceComponents = serviceComponents;
		this.config = config;
	}

	@Override
	public ServiceDiscovery create(AstrixBeanKey<?> key, ConfigDiscoveryProperties configDiscoveryProperties) {
		return new ConfigDiscovery(serviceComponents, config, configDiscoveryProperties.getConfigEntryName(), key);
	}

	@Override
	public Class<ConfigDiscoveryProperties> getDiscoveryPropertiesType() {
		return ConfigDiscoveryProperties.class;
	}

	private static class ConfigDiscovery implements ServiceDiscovery {

		private final ServiceComponentRegistry serviceComponents;
		private final AstrixConfig config;
		private final String configEntryName;
		private final AstrixBeanKey<?> beanKey;

		public ConfigDiscovery(ServiceComponentRegistry serviceComponents, AstrixConfig config, String configEntryName,
				AstrixBeanKey<?> beanKey) {
			this.serviceComponents = serviceComponents;
			this.config = config;
			this.configEntryName = configEntryName;
			this.beanKey = beanKey;
		}

		@Override
		public ServiceProviders run() {
			String serviceUri = config.getStringProperty(configEntryName, null).get();
			if (serviceUri == null) {
				return new ServiceProviders(Collections.emptyList());
			}
			return new ServiceProviders(Arrays.asList(buildServiceProperties(serviceUri)));
		}

		@Override
		public String description() {
			return "ConfigDiscovery[" + configEntryName + "]";
		}

		// A serviceUri has the format
		// [component-name:service-provider-properties]
		// Example: gs-remoting:jini://customer-space?groups=my-group
		private ServiceProviderInstanceProperties buildServiceProperties(String serviceUri) {
			int componentNameEndIndex = serviceUri.indexOf(":");
			if (componentNameEndIndex < 0) {
				throw new IllegalArgumentException("Illegal serviceUri: \"" + serviceUri
						+ "\". A serviceUri should have the form [componentName]:[componentSpecificPart]");
			}
			String component = serviceUri.substring(0, componentNameEndIndex);
			String serviceProviderUri = serviceUri.substring(componentNameEndIndex + 1);
			ServiceComponent serviceComponent = getServiceComponent(component);
			ServiceProviderInstanceProperties serviceProperties = serviceComponent.parseServiceProviderUri(serviceProviderUri);
			serviceProperties.setComponent(serviceComponent.getName());
			serviceProperties.setApi(this.beanKey.getBeanType());
			serviceProperties.setQualifier(this.beanKey.getQualifier());
			return serviceProperties;
		}

		private ServiceComponent getServiceComponent(String componentName) {
			return serviceComponents.getComponent(componentName);
		}

	}

}
