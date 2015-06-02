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
package com.avanza.astrix.ft;

import java.util.List;

import com.avanza.astrix.beans.publish.AstrixBeanDefinition;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanFaultToleranceProxyFactory {
	
	private BeanFaultToleranceProxyProvider faultToleranceProxyProvider;

	public BeanFaultToleranceProxyFactory(List<BeanFaultToleranceProxyProvider> faultToleranceProxyProviders) {
		if (faultToleranceProxyProviders.isEmpty()) {
			this.faultToleranceProxyProvider = new NoFaultToleranceProvider();
		} else {
			this.faultToleranceProxyProvider = faultToleranceProxyProviders.get(0);
		}
	}

	public <T> T addFaultTolerance(final AstrixBeanDefinition<T> beanDefinition, T provider) {
		return faultToleranceProxyProvider.addFaultToleranceProxy(beanDefinition, provider);
	}
	
	private static final class NoFaultToleranceProvider implements BeanFaultToleranceProxyProvider {
		@Override
		public <T> T addFaultToleranceProxy(
				AstrixBeanDefinition<T> beanDefinition, T rawProvider) {
			return rawProvider;
		}
	}
}
