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
package com.avanza.astrix.beans.core;

import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.util.ReflectionUtil;
import rx.Observable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class BeanInvocationDispatcher implements InvocationHandler {
	
	private final List<BeanProxy> proxys;
	private final ReactiveTypeConverter reactiveTypeConverter;
	private final Object targetBean;
	
	public BeanInvocationDispatcher(List<BeanProxy> proxys, ReactiveTypeConverter reactiveTypeConverter, Object targetBean) {
		this.proxys = Objects.requireNonNull(proxys);
		this.reactiveTypeConverter = Objects.requireNonNull(reactiveTypeConverter);
		this.targetBean = Objects.requireNonNull(targetBean);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isObservableType(method.getReturnType()) || isReactiveType(method.getReturnType())) {
			return proxyReactiveInvocation(method, args);
		}
		return proxyInvocation(method, args);
	}
	
	private Object proxyInvocation(final Method method, final Object[] args) throws Throwable {
		CheckedCommand<Object> serviceInvocation = () -> ReflectionUtil.invokeMethod(method, targetBean, args);
		for (BeanProxy proxy : proxys) {
			if (proxy.isEnabled()) {
				serviceInvocation = proxy.proxyInvocation(serviceInvocation);
			}
		}
		return serviceInvocation.call();
	}

	@SuppressWarnings("unchecked")
	private Object proxyReactiveInvocation(final Method method, final Object[] args) {
		Supplier<Observable<Object>> serviceInvocation = () -> {
			try {
				Object reactiveResult = ReflectionUtil.invokeMethod(method, targetBean, args);
				if (isObservableType(method.getReturnType())) {
					return (Observable<Object>) reactiveResult;
				}
				return toObservable(method.getReturnType(), reactiveResult);
			} catch (Throwable e) {
				return Observable.error(e);
			}
		};
		for (BeanProxy proxy : proxys) {
			if (proxy.isEnabled()) {
				serviceInvocation = proxy.proxyReactiveInvocation(serviceInvocation);
			}
		}
		
		if (isObservableType(method.getReturnType())) {
			return serviceInvocation.get(); 
//			return Observable.create((s) -> {
//				serviceInvocation.get().subscribe(s);
//			});
		}
		Observable<Object> asyncResult = serviceInvocation.get();
		return this.reactiveTypeConverter.toCustomReactiveType(method.getReturnType(), asyncResult);
	}

	private <T> Observable<Object> toObservable(Class<T> reactiveType , Object reactiveInstance) {
		return reactiveTypeConverter.toObservable(reactiveType, reactiveType.cast(reactiveInstance));
	}
	
	private boolean isReactiveType(Class<?> type) {
		return this.reactiveTypeConverter.isReactiveType(type);
	}

	private boolean isObservableType(Class<?> type) {
		return Observable.class.isAssignableFrom(type);
	}

}
