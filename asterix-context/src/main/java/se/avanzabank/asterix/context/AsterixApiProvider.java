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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * An AsterixApiProvider holds factories for creating instances of all different elements
 * that is part of an api hooked into asterix. More precisely it holds an AsterixFactoryBean 
 * for each beanType provided by the given api. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixApiProvider {
	
	private final ConcurrentMap<Class<?>, AsterixFactoryBean<?>> factoryByProvidedType = new ConcurrentHashMap<>();
	private final Class<?> descriptorHolder;
	
	public AsterixApiProvider(List<AsterixFactoryBean<?>> factories, Class<?> descriptorHolder) {
		this.descriptorHolder = descriptorHolder;
		for (AsterixFactoryBean<?> factory : factories) {
			AsterixFactoryBean<?> previous = this.factoryByProvidedType.putIfAbsent(factory.getBeanType(), factory);
			if (previous != null) {
				throw new IllegalArgumentException(
						String.format("Duplicate bean factories found. type=%s descriptor=%s",
								factory.getBeanType().getName(), descriptorHolder.getName()));
			}
		}
	}
	
	public Class<?> getDescriptorHolder() {
		return descriptorHolder;
	}

	public Collection<Class<?>> providedApis() {
		return this.factoryByProvidedType.keySet();
	}
	
	public <T> AsterixFactoryBean<T> getFactory(Class<T> type) {
		return (AsterixFactoryBean<T>) this.factoryByProvidedType.get(type);
	}
	
}
