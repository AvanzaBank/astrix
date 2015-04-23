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
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.beans.service.AstrixServiceComponents;
import com.avanza.astrix.beans.service.AstrixServiceLookupMetaFactoryPlugin;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.beans.service.ServiceLookup;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixServiceLookupMetaFactoryPlugin.class)
public class AstrixConfigServiceLookupPlugin implements AstrixServiceLookupMetaFactoryPlugin<AstrixConfigLookup>, AstrixConfigAware {

	private AstrixServiceComponents serviceComponents;
	private DynamicConfig config;
	
	@Override
	public ServiceLookup create(AstrixBeanKey<?> key, AstrixConfigLookup lookupAnnotation) {
		return new ConfigLookup(serviceComponents, config, lookupAnnotation.value());
	}
	
	@Override
	public Class<AstrixConfigLookup> getLookupAnnotationType() {
		return AstrixConfigLookup.class;
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}

	private static class ConfigLookup implements ServiceLookup {

		private AstrixServiceComponents serviceComponents;
		private DynamicConfig config;
		private String configEntryName;
		
		
		public ConfigLookup(AstrixServiceComponents serviceComponents,
				DynamicConfig config, String configEntryName) {
			super();
			this.serviceComponents = serviceComponents;
			this.config = config;
			this.configEntryName = configEntryName;
		}

		@Override
		public AstrixServiceProperties lookup() {
			String serviceUri = config.getStringProperty(configEntryName, null).get();
			if (serviceUri == null) {
				return null;
			}
			return buildServiceProperties(serviceUri);
		}
		
		@Override
		public String description() {
			return "ConfigLookup[" + configEntryName + "]";
		}
		
		private AstrixServiceProperties buildServiceProperties(String serviceUriIncludingComponent) {
			String component = serviceUriIncludingComponent.substring(0, serviceUriIncludingComponent.indexOf(":"));
			String serviceUri = serviceUriIncludingComponent.substring(serviceUriIncludingComponent.indexOf(":") + 1);
			AstrixServiceComponent serviceComponent = getServiceComponent(component);
			AstrixServiceProperties serviceProperties = serviceComponent.createServiceProperties(serviceUri);
			serviceProperties.setComponent(serviceComponent.getName());
			return serviceProperties;
		}
		
		private AstrixServiceComponent getServiceComponent(String componentName) {
			return serviceComponents.getComponent(componentName);
		}

	}

}
