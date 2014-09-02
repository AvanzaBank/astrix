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
package se.avanzabank.service.suite.context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * An AstrixApiProvider holds factories for creating instances of all different elements
 * that is part of an api hooked into astrix. More precisely it holds an AstrixFactoryBean 
 * for each beanType provided by the given api. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixApiProvider {
	
	private final ConcurrentMap<Class<?>, AstrixFactoryBean<?>> factoryByProvidedType = new ConcurrentHashMap<>();
	private final Class<?> descriptorHolder;
	
	public AstrixApiProvider(List<AstrixFactoryBean<?>> factories, Class<?> descriptorHolder) {
		this.descriptorHolder = descriptorHolder;
		for (AstrixFactoryBean<?> factory : factories) {
			AstrixFactoryBean<?> previous = this.factoryByProvidedType.putIfAbsent(factory.getBeanType(), factory);
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
	
	public <T> AstrixFactoryBean<T> getFactory(Class<T> type) {
		return (AstrixFactoryBean<T>) this.factoryByProvidedType.get(type);
	}
	
}
