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

import java.util.ArrayList;
import java.util.Collection;

import com.avanza.astrix.provider.core.AstrixService;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceDescriptor {

	private final Class<?> descriptorHolder;
	private final AstrixService AstrixService;
	private final Collection<AstrixApiDescriptor> apiDescriptors;

	public AstrixServiceDescriptor(AstrixService AstrixService, Class<?> descriptorHolder) {
		this.AstrixService = AstrixService;
		this.descriptorHolder = descriptorHolder;
		this.apiDescriptors = new ArrayList<>(AstrixService.apiDescriptors().length);
		for (Class<?> desc : this.AstrixService.apiDescriptors()) {
			this.apiDescriptors.add(AstrixApiDescriptor.create(desc));
		}
	}
	
	// To simplify creating using spring bean definitions
	public static AstrixServiceDescriptor create(AstrixServiceDescriptor AstrixServiceDescriptor) {
		return AstrixServiceDescriptor;
	}
	
	public static AstrixServiceDescriptor create(Class<?> descriptorHolder) {
		AstrixService AstrixService = descriptorHolder.getAnnotation(AstrixService.class);
		return new AstrixServiceDescriptor(AstrixService, descriptorHolder);
	}
	
	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
	}

	private Class<?> getHolder() {
		return descriptorHolder;
	}

	/**
	 * Default component used for services exported to service registry.
	 * @return
	 */
	public String getComponent() {
		return AstrixService.component();
	}

	public Collection<AstrixApiDescriptor> getApiDescriptors() {
		return this.apiDescriptors;
	}

}
