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
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kristoffer Erlandsson (krierl)
 * @author Elias Lindholm (elilin)
 */
public class HystrixProxyFactory<T> implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(HystrixProxyFactory.class);

	private final T target;
	private HystrixCommandSettings settings;

	private HystrixProxyFactory(T target, HystrixCommandSettings settings) {
		this.target = target;
		this.settings = settings;
	}

	public static <T> T create(Class<T> api, T target, HystrixCommandSettings settings) {
		log.debug("Adding fault tolerance: api={}, group={}", api, settings.getGroupKey());
		if (!api.isInterface()) {
			throw new IllegalArgumentException(
					"Can only add fault tolerance to an api exposed using an interface. Exposed api=" + api);
		}
		return api.cast(Proxy.newProxyInstance(HystrixProxyFactory.class.getClassLoader(), new Class[] { api },
				new HystrixProxyFactory<T>(target, settings)));
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		return new HystrixCommandFacade<>(new CheckedCommand<Object>() {
			@Override
			public Object call() throws Throwable {
				try {
					return method.invoke(target, args);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
		}, settings).execute();		
	}
	
}
