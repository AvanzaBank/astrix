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

import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * Holds information about a given exported service. 
 * 
 * This info fully identifies all relevant information for an exported 
 * service: 
 *  - service-component used (AstrixServiceComponent)
 *  - apiDescriptor
 *  - serviceType (AstrixBeanType)
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixExportedServiceInfo {
	
	private Class<?> providedService;
	private AstrixApiDescriptor apiDescriptor;
	private String componentName;
	private Object provider;
	private ServiceVersioningContext versioningContext;

	public AstrixExportedServiceInfo(Class<?> providedService, AstrixApiDescriptor apiDescriptor, ServiceVersioningContext versioningContext, String componentName, Object provider) {
		this.providedService = providedService;
		this.apiDescriptor = apiDescriptor;
		this.versioningContext = versioningContext;
		this.componentName = componentName;
		this.provider = provider;
	}
	
	public Class<?> getProvidedService() {
		return providedService;
	}
	
	public AstrixApiDescriptor getApiDescriptor() {
		return apiDescriptor;
	}
	
	public String getComponentName() {
		return componentName;
	}
	
	public Object getProvider() {
		return provider;
	}

	public ServiceVersioningContext getVersioningContext() {
		return versioningContext;
	}
	
}
