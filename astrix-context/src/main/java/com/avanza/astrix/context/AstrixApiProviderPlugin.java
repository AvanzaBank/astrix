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

import java.lang.annotation.Annotation;
import java.util.List;


/**
 * An AstrixApiProviderPlugin is responsible for creating AstrixFactoryBeanPlugin for
 * all parts of a given "type" of api. By "type" in this context, we don't mean the
 * different api's that are hooked into Astrix for consumption, but rather a mechanism
 * for an api-provider to publish the different part's of the api, thereby allowing it
 * to be consumed using Astrix.
 * 
 * For instance, one type of api is "library", which is handled by the {@link AstrixLibraryProviderPlugin}. It
 * allows api-providers to export api's that require "a lot" of wiring on the client side by annotating
 * a class with @AstrixServiceProvider an export different api-elements by annotating factory methods
 * for different api elements with @AstrixExport. 
 * 
 * Another type of api is a "service" api, which binds to services using an {@link AstrixServiceComponent}.
 * This typically also requires a server side component to respond to the service-invocation-request.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixApiProviderPlugin {
	
	List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptor);
	
	List<Class<?>> getProvidedBeans(AstrixApiDescriptor descriptor);
	
	Class<? extends Annotation> getProviderAnnotationType();
	
	/**
	 * Defines whether the beans provided by this type of api are stateful. 
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
	boolean hasStatefulBeans();
	
	
}