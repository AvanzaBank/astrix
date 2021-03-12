/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.publish;

import java.lang.annotation.Annotation;

import com.avanza.astrix.provider.core.AstrixApiProvider;


/**
 * A BeanPublisherPlugin is responsible for creating a {@link ServiceBeanDefinition} for
 * all published types in an api (represented by a ApiProviderClass). 
 * 
 * Astrix comes with the ApiProviderBeanPublisherPlugin which handles api-providers
 * annotated with {@link AstrixApiProvider}, see {@link AstrixApiProvider} for more information.<p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface BeanPublisherPlugin {
	
	void publishBeans(BeanPublisher p, ApiProviderClass apiProviderClass);
	
	Class<? extends Annotation> getProviderAnnotationType();
	
	public static interface BeanPublisher {
		<T> void publishLibrary(LibraryBeanDefinition<T> libraryDefinition);
		<T> void publishService(ServiceBeanDefinition<T> serviceDefinition);
	}
	
}