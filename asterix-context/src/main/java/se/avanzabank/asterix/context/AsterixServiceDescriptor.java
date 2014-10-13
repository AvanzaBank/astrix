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
package se.avanzabank.asterix.context;

import java.util.HashMap;
import java.util.Map;

import se.avanzabank.asterix.provider.core.AsterixService;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceDescriptor {

	private final Class<?> descriptorHolder;
	private final AsterixService asterixService;
	private final Map<Class<?>, AsterixApiDescriptor> apiDescriptorByProvideService;
	private final String subsystem;

	public AsterixServiceDescriptor(AsterixService asterixService, Class<?> descriptorHolder, Map<Class<?>, AsterixApiDescriptor> apiDescriptorByProvideService) {
		this.asterixService = asterixService;
		this.descriptorHolder = descriptorHolder;
		this.apiDescriptorByProvideService = apiDescriptorByProvideService;
		this.subsystem = asterixService.subsystem();
	}
	
	// To simplify creating using srping bean definitions
	public static AsterixServiceDescriptor create(AsterixServiceDescriptor asterixServiceDescriptor) {
		return asterixServiceDescriptor;
	}
	
	public static AsterixServiceDescriptor create(Class<?> descriptorHolder, AsterixContext context) {
		AsterixService asterixService = descriptorHolder.getAnnotation(AsterixService.class);
		Map<Class<?>, AsterixApiDescriptor> apiDescriptorByProvideService = new HashMap<>();
		for (Class<?> apiDescriptorHolder : asterixService.apiDescriptors()) {
			AsterixApiDescriptor apiDescriptor = new AsterixApiDescriptor(apiDescriptorHolder);
			for (Class<?> beanType : context.getExportedBeans(apiDescriptor)) {
				apiDescriptorByProvideService.put(beanType, apiDescriptor);
			}
		}
		return new AsterixServiceDescriptor(asterixService, descriptorHolder, apiDescriptorByProvideService);
	}
	
	@Override
	public String toString() {
		return descriptorHolder.getName().toString();
	}

	private Class<?> getHolder() {
		return descriptorHolder;
	}

	public AsterixApiDescriptor getApiDescriptor(Class<?> serviceType) {
		AsterixApiDescriptor result = this.apiDescriptorByProvideService.get(serviceType);
		if (result == null) {
			throw new IllegalArgumentException("Service descriptor does not export service. descriptor: " + getHolder().getName() + ", service: " + serviceType.getName());
		}
		return result;
	}

	/**
	 * Default component used for services exported to service registry.
	 * @return
	 */
	public String getComponent() {
		return asterixService.component();
	}

	public boolean publishesService(Class<?> providedServiceType) {
		return this.apiDescriptorByProvideService.containsKey(providedServiceType);
	}
	
}
