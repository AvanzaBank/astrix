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

/**
 * Strategy for creating a given bean type.
 * 
 * See {@link AstrixApiProviderPlugin}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface AstrixFactoryBeanPlugin<T> {
	
	T create();
	
	AstrixBeanKey<T> getBeanKey();
	
	/**
	 * Defines whether the bean created by this factory is stateful.  
	 * 
	 * A stateful bean may not possible to create at a given time. For instance, for service's 
	 * provided using the service registry it might not be possible to create the bean for any of these reasons:
	 * 
	 * 1. The service-registry i not available
	 * 2. No service provider is registered in the registry (yet)
	 * 3. The service provider does not respond on connection attempt
	 * 
	 * Astrix will manage the state for stateful beans and make sure it's always possible
	 * to create an instance of a stateful beans (using Astrix.getBean). The bean will 
	 * be in UNBOUND state if the underlying AstrixFactoryBeanPlugin can't create the bean. 
	 * Astrix will periodically attempt to create the bean using the underlying factory
	 * until successful.

	 * @return
	 */
	boolean isStateful();
	
}
