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
import java.util.concurrent.Future;

import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.FutureAdapter;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.core.util.ReflectionUtil;

import rx.Observable;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class BeanFaultToleranceImpl {

	private final DynamicBooleanProperty faultToleranceEnabledForBean;
	private final DynamicBooleanProperty faultToleranceEnabled;
	private final FaultToleranceSpi beanFaultToleranceSpi;
	private final CommandSettings commandSettings;
	
	BeanFaultToleranceImpl(BeanConfiguration beanConfiguration, DynamicConfig config, FaultToleranceSpi beanFaultToleranceSpi,
			CommandSettings commandSettings) {
		this.beanFaultToleranceSpi = beanFaultToleranceSpi;
		this.commandSettings = commandSettings;
		this.faultToleranceEnabledForBean = beanConfiguration.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED);
		this.faultToleranceEnabled = AstrixSettings.ENABLE_FAULT_TOLERANCE.getFrom(config);
	}
	
	public <T> T addFaultToleranceProxy(final Class<T> api, final T target) {
		return ReflectionUtil.newProxy(api, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (!faultToleranceEnabled()) {
					return ReflectionUtil.invokeMethod(method, target, args);
				}
				if (isObservableType(method.getReturnType()) || isFutureType(method.getReturnType())) {
					return observe(target, method, args);
				}
				return execute(target, method, args);
			}
		});
	}
	
	private Object execute(final Object target, final Method method, final Object[] args) throws Throwable {
		return execute(new CheckedCommand<Object>() {
			@Override
			public Object call() throws Throwable {
				return ReflectionUtil.invokeMethod(method, target, args);
			};
		});
	}

	private Object observe(final Object target, final Method method, final Object[] args) {
		Observable<Object> faultToleranceProtectedResult = observe(new Supplier<Observable<Object>>() {
			@Override
			public Observable<Object> get() {
				try {
					Object asyncResult = ReflectionUtil.invokeMethod(method, target, args);
					if (isObservableType(method.getReturnType())) {
						 return (Observable<Object>) asyncResult;
					}
					return Observable.<Object>from((Future) asyncResult);
				} catch (Throwable e) {
					return Observable.error(e);
				}
			}
		});
		if (isFutureType(method.getReturnType())) {
			return new FutureAdapter<>(faultToleranceProtectedResult);
		}
		return faultToleranceProtectedResult;
	}
	

	private <T> Observable<T> observe(Supplier<Observable<T>> observable) {
		return beanFaultToleranceSpi.observe(observable, commandSettings);
	}
	
	private <T> T execute(final CheckedCommand<T> command) throws Throwable {
		return beanFaultToleranceSpi.execute(command, commandSettings);
	}
	
	private boolean isFutureType(Class<?> type) {
		return Future.class.isAssignableFrom(type);
	}

	private boolean isObservableType(Class<?> type) {
		return Observable.class.isAssignableFrom(type);
	}

	
	private <T> boolean faultToleranceEnabled() {
		return faultToleranceEnabled.get() && faultToleranceEnabledForBean.get();
	}

}
