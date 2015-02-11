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

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixServiceLookupPlugin.class)
public class AstrixConfigServiceLookupPlugin implements AstrixServiceLookupPlugin<AstrixConfigLookup>, AstrixConfigAware {

	private AstrixServiceComponents serviceComponents;
	private DynamicConfig config;

	@Override
	public AstrixServiceProperties lookup(Class<?> beanType, String optionalQualifier, AstrixConfigLookup lookupAnnotation) {
		String serviceUri = config.getStringProperty(lookupAnnotation.value(), null).get();
		if (serviceUri == null) {
			return null;
		}
		return buildServiceProperties(serviceUri);
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


}
