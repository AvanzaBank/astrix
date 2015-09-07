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
package com.avanza.astrix.context.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.avanza.astrix.beans.factory.BeanProxy;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.util.ReflectionUtil;

import rx.Observable;

public final class BeanInvocationDispatcher implements InvocationHandler {
	
	private final List<BeanProxy> proxys;
	private final AsyncTypeConverter asyncTypeConverter;
	private final Object targetBean;
	
	public BeanInvocationDispatcher(List<BeanProxy> proxys, AsyncTypeConverter asyncTypeConverter, Object targetBean) {
		this.proxys = Objects.requireNonNull(proxys);
		this.asyncTypeConverter = Objects.requireNonNull(asyncTypeConverter);
		this.targetBean = Objects.requireNonNull(targetBean);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isObservableType(method.getReturnType()) || isAsyncType(method.getReturnType())) {
			return proxyAsyncInvocation(method, args);
		}
		return proxyInvocation(method, args);
	}
	
	private Object proxyInvocation(final Method method, final Object[] args) throws Throwable {
		CheckedCommand<Object> serviceInvocation = () -> ReflectionUtil.invokeMethod(method, targetBean, args);
		for (BeanProxy proxy : proxys) {
			serviceInvocation = proxy.proxyInvocation(serviceInvocation);
		}
		return serviceInvocation.call();
	}

	private Object proxyAsyncInvocation(final Method method, final Object[] args) {
		Supplier<Observable<Object>> serviceInvocation = () -> {
			try {
				Object asyncResult = ReflectionUtil.invokeMethod(method, targetBean, args);
				if (isObservableType(method.getReturnType())) {
					return (Observable<Object>) asyncResult;
				}
				return asyncTypeConverter.toObservable(method.getReturnType(), asyncResult);
			} catch (Throwable e) {
				return Observable.error(e);
			}
		};
		for (BeanProxy proxy : proxys) {
			serviceInvocation = proxy.proxyAsyncInvocation(serviceInvocation);
		}
		Observable<Object> asyncResult = serviceInvocation.get();
		if (isObservableType(method.getReturnType())) {
			return asyncResult;
		}
		return this.asyncTypeConverter.toAsyncType(method.getReturnType(), asyncResult);
	}
	
	private boolean isAsyncType(Class<?> type) {
		return this.asyncTypeConverter.canAdaptToType(type);
	}

	private boolean isObservableType(Class<?> type) {
		return Observable.class.isAssignableFrom(type);
	}

}
