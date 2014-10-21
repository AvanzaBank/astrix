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

import java.util.ArrayList;
import java.util.Collection;

import se.avanzabank.asterix.provider.core.AsterixService;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceDescriptor {

	private final Class<?> descriptorHolder;
	private final AsterixService asterixService;
	private final Collection<AsterixApiDescriptor> apiDescriptors;

	public AsterixServiceDescriptor(AsterixService asterixService, Class<?> descriptorHolder) {
		this.asterixService = asterixService;
		this.descriptorHolder = descriptorHolder;
		this.apiDescriptors = new ArrayList<>(asterixService.apiDescriptors().length);
		for (Class<?> desc : this.asterixService.apiDescriptors()) {
			this.apiDescriptors.add(new AsterixApiDescriptor(desc));
		}
	}
	
	// To simplify creating using srping bean definitions
	public static AsterixServiceDescriptor create(AsterixServiceDescriptor asterixServiceDescriptor) {
		return asterixServiceDescriptor;
	}
	
	public static AsterixServiceDescriptor create(Class<?> descriptorHolder) {
		AsterixService asterixService = descriptorHolder.getAnnotation(AsterixService.class);
		return new AsterixServiceDescriptor(asterixService, descriptorHolder);
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
		return asterixService.component();
	}

	public Collection<AsterixApiDescriptor> getApiDescriptors() {
		return this.apiDescriptors;
	}

	public String getSubsystem() {
		return this.asterixService.subsystem();
	}
	
}
