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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestAsterixConfigurer {
	
	private AsterixConfigurer configurer;
	private final Set<AsterixApiDescriptor> descriptors = new HashSet<>();
	
	public TestAsterixConfigurer() {
		configurer = new AsterixConfigurer();
		configurer.setAsterixApiDescriptors(new AsterixApiDescriptors() {
			@Override
			public Collection<AsterixApiDescriptor> getAll() {
				return descriptors;
			}
		});
		configurer.enableVersioning(false);
		configurer.enableFaultTolerance(false);
		configurer.enableMonitoring(false);
		configurer.set(AsterixSettings.ENFORECE_SUBSYSTEM_BOUNDARIES, false);
	}

	public AsterixContext configure() {
		return configurer.configure();
	}

	public void registerApiDescriptor(Class<?> descriptor) {
		descriptors.add(new AsterixApiDescriptor(descriptor));
	}

	public <T> void registerPlugin(Class<T> c, T provider) {
		configurer.registerPlugin(c, provider);
	}
	
	public void enableMonitoring(boolean enabled) {
		configurer.enableMonitoring(enabled);
	}
	
	public void enableFaultTolerance(boolean enabled) {
		configurer.enableFaultTolerance(enabled);
	}

	public void set(String name, long value) {
		configurer.set(name, value);
	}
	
	
}
