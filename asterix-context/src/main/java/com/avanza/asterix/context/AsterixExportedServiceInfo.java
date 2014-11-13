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
package com.avanza.asterix.context;
/**
 * Holds information about a given exported service. 
 * 
 * This info fully identifies all relevant information for an exported 
 * service: 
 *  - service-component used (AsterixServiceComponent)
 *  - apiDescriptor
 *  - serviceType (asterixBeanType)
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixExportedServiceInfo {
	
	private Class<?> providedService;
	private AsterixApiDescriptor apiDescriptor;
	private String componentName;
	private String providingBeanName;

	public AsterixExportedServiceInfo(Class<?> providedService,
			AsterixApiDescriptor apiDescriptor, String componentName, String providingBeanName) {
		this.providedService = providedService;
		this.apiDescriptor = apiDescriptor;
		this.componentName = componentName;
		this.providingBeanName = providingBeanName;
	}
	
	public Class<?> getProvidedService() {
		return providedService;
	}
	
	public AsterixApiDescriptor getApiDescriptor() {
		return apiDescriptor;
	}
	
	public String getComponentName() {
		return componentName;
	}
	
	public String getProvidingBeanName() {
		return providingBeanName;
	}
	
}
