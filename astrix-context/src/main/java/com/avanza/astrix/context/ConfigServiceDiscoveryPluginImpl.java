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
package com.avanza.astrix.context;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.beans.service.ServiceDiscovery;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactoryPlugin;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ConfigServiceDiscoveryPluginImpl implements ServiceDiscoveryMetaFactoryPlugin<AstrixConfigDiscovery> {

	private ServiceComponentRegistry serviceComponents;
	private AstrixConfig config;
	
	public ConfigServiceDiscoveryPluginImpl(ServiceComponentRegistry serviceComponents, AstrixConfig config) {
		this.serviceComponents = serviceComponents;
		this.config = config;
	}

	@Override
	public ServiceDiscovery create(AstrixBeanKey<?> key, AstrixConfigDiscovery lookupAnnotation) {
		return new ConfigDiscovery(serviceComponents, config, lookupAnnotation.value(), key);
	}
	
	@Override
	public Class<AstrixConfigDiscovery> getDiscoveryAnnotationType() {
		return AstrixConfigDiscovery.class;
	}

	private static class ConfigDiscovery implements ServiceDiscovery {

		private ServiceComponentRegistry serviceComponents;
		private AstrixConfig config;
		private String configEntryName;
		private AstrixBeanKey<?> beanKey;
		
		
		public ConfigDiscovery(ServiceComponentRegistry serviceComponents,
				AstrixConfig config, String configEntryName, AstrixBeanKey<?> beanKey) {
			this.serviceComponents = serviceComponents;
			this.config = config;
			this.configEntryName = configEntryName;
			this.beanKey = beanKey;
		}

		@Override
		public ServiceProperties run() {
			String serviceUri = config.getStringProperty(configEntryName, null).get();
			if (serviceUri == null) {
				return null;
			}
			return buildServiceProperties(serviceUri);
		}
		
		@Override
		public String description() {
			return "ConfigDiscovery[" + configEntryName + "]";
		}
		
		// A serviceUri has the format [component-name:service-provider-properties]
		// Example: gs-remoting:jini://customer-space?groups=my-group
		private ServiceProperties buildServiceProperties(String serviceUri) {
			String component = serviceUri.substring(0, serviceUri.indexOf(":"));
			String serviceProviderUri = serviceUri.substring(serviceUri.indexOf(":") + 1);
			ServiceComponent serviceComponent = getServiceComponent(component);
			ServiceProperties serviceProperties = serviceComponent.parseServiceProviderUri(serviceProviderUri);
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
