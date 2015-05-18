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

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Service;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface Astrix {

	/**
	 * Lookup an unqualified bean of a given type in the bean registry. <p>
	 * 
	 * This will trigger instantiation of the given bean, if it has not bean requested before.
	 * 
	 * @param beanType
	 * @return
	 */
	<T> T getBean(Class<T> beanType);

	/**
	 * Same as {@link #getBean(Class)}, but will lookup a qualified bean, see {@link AstrixQualifier}.
	 * 
	 * @param beanType
	 * @param qualifier
	 * @return
	 */
	<T> T getBean(Class<T> beanType, String qualifier);

	/**
	 * Same as {@link #getBean(Class)}, but will also block until all transitive service beans
	 * are bound, see {@link Service}. 
	 * 
	 * @param beanType
	 * @param timeoutMillis
	 * @return
	 * @throws InterruptedException
	 * @throws {@link ServiceUnavailableException} - if all transitive dependencies was not bound before timeout.
	 */
	<T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException;

	/**
	 * 
	 * Same as {@link #getBean(Class, String)}, but will also block until all transitive service beans
	 * are bound, see {@link Service}.
	 * 
	 * @param beanType
	 * @param qualifier
	 * @param timeoutMillis
	 * @return
	 * @throws InterruptedException
	 * @throws {@link ServiceUnavailableException} - if all transitive dependencies was not bound before timeout.
	 */
	<T> T waitForBean(Class<T> beanType, String qualifier,long timeoutMillis) throws InterruptedException;

}
