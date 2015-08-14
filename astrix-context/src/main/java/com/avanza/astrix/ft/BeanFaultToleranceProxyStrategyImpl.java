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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.core.util.ReflectionUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class BeanFaultToleranceProxyStrategyImpl implements BeanFaultToleranceProxyStrategy {
	
	private final BeanFaultToleranceFactory faultToleranceFactory;
	
	public BeanFaultToleranceProxyStrategyImpl(BeanFaultToleranceFactory faultToleranceFactory) {
		this.faultToleranceFactory = faultToleranceFactory;
	}
	
	@Override
	public <T> T addFaultToleranceProxy(PublishedAstrixBean<T> beanDefinition, T rawProvider) {
		return ReflectionUtil.newProxy(beanDefinition.getBeanKey().getBeanType(), 
									   new BeanFaultToleranceProxy(rawProvider, faultToleranceFactory.create(beanDefinition)));
	}
	
	private static class BeanFaultToleranceProxy implements InvocationHandler {

		private final Object provider;
		private final BeanFaultTolerance faultTolerance;
		
		public BeanFaultToleranceProxy(Object rawProvider, BeanFaultTolerance faultTolerance) {
			this.provider = rawProvider;
			this.faultTolerance = faultTolerance;
		}

		@Override
		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			return faultTolerance.execute(new CheckedCommand<Object>() {
				@Override
				public Object call() throws Throwable {
					return ReflectionUtil.invokeMethod(method, provider, args);
				}
			});
		}

	}

}
