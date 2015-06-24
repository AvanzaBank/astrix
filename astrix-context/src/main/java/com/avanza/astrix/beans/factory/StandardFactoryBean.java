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
package com.avanza.astrix.beans.factory;

import com.avanza.astrix.beans.core.AstrixBeanKey;


/**
 * FactoryBean strategy used by {@link AstrixBeanFactory} to create a bean
 * in the factory. The bean created by this factory is locked to a single
 * {@link AstrixBeanKey}, as opposed to the beans created by {@link DynamicFactoryBean}
 * which can create arbitrary beans of a given type. 
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface StandardFactoryBean<T> extends FactoryBean<T> {
	
	// Rename to PredefinedKeyFactoryBean?
	
	/**
	 * Creates the bean. Dependencies
	 * required by the bean may be pulled out
	 * from the {@link AstrixBeans} argument, which
	 * will trigger creation (or returned a cached instance)
	 * of the requested bean in the given {@link AstrixBeanFactory}. 
	 * 
	 * @throws CircularDependency
	 * 			if requesting a bean from the passed in {@link AstrixBeans} that is	currently 
	 * 	        being created and in turn triggered creation of the bean associated with this
	 *          factory.
	 *          
	 * @param beans
	 * @return
	 */
	T create(AstrixBeans beans);
	
	boolean lifecycled();
	
	/**
	 * Identifier for the bean created by this factory.
	 * 
	 * @return
	 */
	AstrixBeanKey<T> getBeanKey();
	
}
