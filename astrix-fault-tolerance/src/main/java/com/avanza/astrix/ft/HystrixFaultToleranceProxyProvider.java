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
package com.avanza.astrix.ft;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.core.util.ReflectionUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(FaultToleranceProxyProvider.class)
public class HystrixFaultToleranceProxyProvider implements FaultToleranceProxyProvider {
	
	private AstrixFaultTolerance faultTolerance;
	
	@AstrixInject
	public void setFaultTolerance(AstrixFaultTolerance faultTolerance) {
		this.faultTolerance = faultTolerance;
	}

	@Override
	public <T> T addFaultToleranceProxy(Class<T> type, T rawProvider, HystrixCommandKeys commandKeys) {
		HystrixCommandSettings settings = new HystrixCommandSettings(commandKeys.getCommandKey(), commandKeys.getGroupKey());
		return ReflectionUtil.newProxy(type, new HystrixFaultToleranceProxy(rawProvider, faultTolerance, settings));
	}
	
	private static class HystrixFaultToleranceProxy implements InvocationHandler {

		private final Object provider;
		private final AstrixFaultTolerance faultTolerance;
		private final HystrixCommandSettings settings;
		
		public HystrixFaultToleranceProxy(Object rawProvider,
				AstrixFaultTolerance faultTolerance, HystrixCommandSettings settings) {
			this.provider = rawProvider;
			this.faultTolerance = faultTolerance;
			this.settings = settings;
		}

		@Override
		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			return faultTolerance.execute(new CheckedCommand<Object>() {
				@Override
				public Object call() throws Throwable {
					try {
						return method.invoke(provider, args);
					} catch (InvocationTargetException e) {
						throw e.getTargetException();
					}
				}
			}, settings);
		}


	}

}
