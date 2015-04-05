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
package com.avanza.astrix.serviceunit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.avanza.astrix.beans.publish.AstrixApiProviderClass;
import com.avanza.astrix.provider.core.AstrixApplication;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApplicationDescriptor {

	private final Class<?> descriptorHolder;
	private final String defaultServiceComponent;
	private final Collection<AstrixApiProviderClass> exportsRemoteServicesFor;

	
	private AstrixApplicationDescriptor(Class<?> descriptorHolder, String defaultServiceComponent, Collection<AstrixApiProviderClass> exportsRemoteServicesFor) {
		this.descriptorHolder = descriptorHolder;
		this.defaultServiceComponent = defaultServiceComponent;
		this.exportsRemoteServicesFor = exportsRemoteServicesFor;
	}
	
	public static AstrixApplicationDescriptor create(Class<?> applicationDescriptorHolder) {
		String defaultServiceComponent;
		if (!applicationDescriptorHolder.isAnnotationPresent(AstrixApplication.class)) {
			throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
					" descriptor must be annotated with @AstrixApplication. descriptorClass=" + applicationDescriptorHolder.getName());
		}
		AstrixApplication astrixApplication = applicationDescriptorHolder.getAnnotation(AstrixApplication.class);
		defaultServiceComponent = getDefaultServiceComponent(astrixApplication, applicationDescriptorHolder);
		Class<?>[] exportedRemoteServiceEndpointClasses = getExportedRemoteServiceEndpoints(astrixApplication, applicationDescriptorHolder);
		Set<AstrixApiProviderClass> exportsRemoteServicesForApis = new HashSet<>();
		for (Class<?> apiProviderClass : exportedRemoteServiceEndpointClasses) {
			exportsRemoteServicesForApis.add(AstrixApiProviderClass.create(apiProviderClass));
		}
		return new AstrixApplicationDescriptor(applicationDescriptorHolder, defaultServiceComponent, exportsRemoteServicesForApis);
	}

	private static String getDefaultServiceComponent(
			AstrixApplication astrixApplication, Class<?> applicationDescriptorHolder) {
		if (astrixApplication.defaultServiceComponent().isEmpty() && 
				astrixApplication.component().isEmpty()) {
			throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
					" descriptor must not define both a 'component' property and a 'defaultComponentName'"
					+ " property. applicationDescriptorClass=" + applicationDescriptorHolder.getName());
		}
		if (!astrixApplication.defaultServiceComponent().isEmpty()) {
			return astrixApplication.defaultServiceComponent();
		}
		if (!astrixApplication.component().isEmpty()) {
			return astrixApplication.component();
		}
		throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
				" descriptor must define the 'defaultComponentName' property");
	}

	private static Class<?>[] getExportedRemoteServiceEndpoints(AstrixApplication astrixApplication, Class<?> applicationDescriptorHolder) {
		if (astrixApplication.apiDescriptors().length > 0 && 
				astrixApplication.exportsRemoteServicesFor().length > 0) {
			throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
					" descriptor must not define both a 'apiDescriptors' property and a 'exportsRemoteServicesFor'"
					+ " property. applicationDescriptorClass=" + applicationDescriptorHolder.getName());
		}
		if (astrixApplication.exportsRemoteServicesFor().length > 0) {
			return astrixApplication.exportsRemoteServicesFor();
		}
		if (astrixApplication.apiDescriptors().length > 0) {
			return astrixApplication.apiDescriptors();
		}
		throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
				" descriptor must define the 'exportsRemoteServicesFor' property");
	}
	
	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
	}

	/**
	 * Default AstrixServiceComponent used for services exported to service registry.
	 * @return
	 */
	public String getDefaultServiceComponent() {
		return defaultServiceComponent;
	}

	/**
	 * Defines what api's that this application should export remote service endpoints
	 * for. <p>
	 * @return
	 */
	public Collection<AstrixApiProviderClass> exportsRemoteServicesFor() {
		return this.exportsRemoteServicesFor;
	}

}
