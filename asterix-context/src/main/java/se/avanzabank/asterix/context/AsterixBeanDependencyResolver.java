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

public class AsterixBeanDependencyResolver {
	
	private AsterixContext context;
	
	public AsterixBeanDependencyResolver(AsterixContext context) {
		this.context = context;
	}
	
	/**
	 * Resolves what beans are required to create a given set beanTypes (ie 
	 * discover all transitive bean dependencies).
	 * 
	 * @param beanTypes
	 * @return
	 */
	public Collection<Class<?>> resolveTransitiveBeanDependencies(Collection<Class<?>> beanTypes) {
		Set<Class<?>> result = new HashSet<>();
		for (Class<?> consumedBeanType : beanTypes) {
			result.add(consumedBeanType);
			Collection<Class<?>> transitiveDependencies = context.getTransitiveBeanDependenciesForBean(consumedBeanType);
			result.addAll(resolveTransitiveBeanDependencies(transitiveDependencies));
		}
		return result;
	}
	
}
