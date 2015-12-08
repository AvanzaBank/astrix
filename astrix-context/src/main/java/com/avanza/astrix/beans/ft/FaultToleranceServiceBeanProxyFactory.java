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
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.service.ServiceBeanProxyFactory;
/**
 * 
 * @author Elias Lindholm
 *
 */
final class FaultToleranceServiceBeanProxyFactory implements ServiceBeanProxyFactory {
	
	private final BeanFaultToleranceFactory ftFactory;
	
	public FaultToleranceServiceBeanProxyFactory(BeanFaultToleranceFactory ftFactory) {
		this.ftFactory = ftFactory;
	}

	@Override
	public BeanProxy create(AstrixBeanKey<?> beanKey) {
		return ftFactory.createFaultToleranceProxy(beanKey);
	}
	
	@Override
	public int order() {
		return 1;
	}
	
}
