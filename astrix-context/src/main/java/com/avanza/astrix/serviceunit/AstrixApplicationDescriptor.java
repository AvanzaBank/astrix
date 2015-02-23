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

import com.avanza.astrix.beans.publish.AstrixApiDescriptor;
import com.avanza.astrix.provider.core.AstrixApplication;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApplicationDescriptor {

	private final Class<?> descriptorHolder;
	private final String component;
	private final Collection<AstrixApiDescriptor> apiDescriptors;

	
	private AstrixApplicationDescriptor(Class<?> descriptorHolder, String component, Collection<AstrixApiDescriptor> apiDescriptors) {
		this.descriptorHolder = descriptorHolder;
		this.component = component;
		this.apiDescriptors = apiDescriptors;
	}
	
	public static AstrixApplicationDescriptor create(Class<?> applicationDescriptorHolder) {
		String component;
		Class<?>[] apiDescriptorHolders;
		if (applicationDescriptorHolder.isAnnotationPresent(AstrixApplication.class)) {
			AstrixApplication astrixApplication = applicationDescriptorHolder.getAnnotation(AstrixApplication.class);
			component = astrixApplication.component();
			apiDescriptorHolders = astrixApplication.apiDescriptors();
		} else {
			throw new IllegalArgumentException("Illegal applicationDescriptor. An application" +
					" descriptor must be annotated with @AstrixApplication. descriptorClass=" + applicationDescriptorHolder.getName());
		}
		Set<AstrixApiDescriptor> apiDescriptors = new HashSet<>();
		for (Class<?> apiDescriptorHolder : apiDescriptorHolders) {
			apiDescriptors.add(AstrixApiDescriptor.create(apiDescriptorHolder));
		}
		return new AstrixApplicationDescriptor(applicationDescriptorHolder, component, apiDescriptors);
	}
	
	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
	}

	/**
	 * Default component used for services exported to service registry.
	 * @return
	 */
	public String getComponent() {
		return component;
	}

	public Collection<AstrixApiDescriptor> getApiDescriptors() {
		return this.apiDescriptors;
	}

}
