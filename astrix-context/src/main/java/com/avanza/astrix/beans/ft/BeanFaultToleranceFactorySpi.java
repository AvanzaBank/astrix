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
package com.avanza.astrix.beans.ft;

import com.avanza.astrix.beans.core.AstrixBeanKey;

public interface BeanFaultToleranceFactorySpi {

	/**
	 * Creates a {@link BeanFaultTolerance} instance associated with a given AstrixBeanKey<?>.
	 * 
	 * Invocations protected by a BeanFaultTolerance instance with different {@link AstrixBeanKey} should
	 * be isolated from each other, which typically means that they should have a distinct bulk-head and
	 * circuit breaker.
	 * 
	 * @param beanKey
	 * @return
	 */
	BeanFaultTolerance create(AstrixBeanKey<?> beanKey);

}
