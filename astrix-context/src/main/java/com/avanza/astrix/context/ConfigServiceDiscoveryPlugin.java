/*
 * Copyright 2014-2015 Avanza Bank AB
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

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceComponents;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactoryPlugin;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.ServiceDiscovery;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(ServiceDiscoveryMetaFactoryPlugin.class)
public class ConfigServiceDiscoveryPlugin implements ServiceDiscoveryMetaFactoryPlugin<AstrixConfigLookup>, AstrixConfigAware {

	private ServiceComponents serviceComponents;
	private DynamicConfig config;
	
	@Override
	public ServiceDiscovery create(AstrixBeanKey<?> key, AstrixConfigLookup lookupAnnotation) {
		return new ConfigDiscovery(serviceComponents, config, lookupAnnotation.value());
	}
	
	@Override
	public Class<AstrixConfigLookup> getDiscoveryAnnotationType() {
		return AstrixConfigLookup.class;
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	@AstrixInject
	public void setServiceComponents(ServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}

	private static class ConfigDiscovery implements ServiceDiscovery {

		private ServiceComponents serviceComponents;
		private DynamicConfig config;
		private String configEntryName;
		
		
		public ConfigDiscovery(ServiceComponents serviceComponents,
				DynamicConfig config, String configEntryName) {
			super();
			this.serviceComponents = serviceComponents;
			this.config = config;
			this.configEntryName = configEntryName;
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
		
		private ServiceProperties buildServiceProperties(String serviceUriIncludingComponent) {
			String component = serviceUriIncludingComponent.substring(0, serviceUriIncludingComponent.indexOf(":"));
			String serviceUri = serviceUriIncludingComponent.substring(serviceUriIncludingComponent.indexOf(":") + 1);
			ServiceComponent serviceComponent = getServiceComponent(component);
			ServiceProperties serviceProperties = serviceComponent.createServiceProperties(serviceUri);
			serviceProperties.setComponent(serviceComponent.getName());
			return serviceProperties;
		}
		
		private ServiceComponent getServiceComponent(String componentName) {
			return serviceComponents.getComponent(componentName);
		}

	}

}
