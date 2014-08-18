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
package se.avanzabank.service.suite.ft;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;

public class HystrixAdapter<T> implements InvocationHandler {

	private T provider;
	private Class<T> api;

	public HystrixAdapter(Class<T> api, T provider) {
		this.api = api;
		this.provider = provider;
	}

	public static <T> T create(Class<T> api, T provider) {
		return api.cast(Proxy.newProxyInstance(HystrixAdapter.class.getClassLoader(), new Class[]{api}, new HystrixAdapter<T>(api, provider)));
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		/*
		 * TODO: inspect method an use correct isolation strategy:
		 * 
		 *   1. Use HystrixObservableCommand with semaphore isolation for non-blocking operations (methods returning Observable's/Futures)
		 *   2. Use HystrixCommand with thread isolation for all blocking operations (any method NOT returning an Observable/Future)
		 */
		try {
			return new HystrixCommand<Object>(getCommandGroupKey()) {
				@Override
				protected Object run() throws Exception {
					return method.invoke(provider, args);
				}
			}.execute();
		} catch (HystrixRuntimeException e) {
			InvocationTargetException ex = (InvocationTargetException) e.getCause();
			throw ex.getCause();
		}
	}

	private HystrixCommandGroupKey getCommandGroupKey() {
		return HystrixCommandGroupKey.Factory.asKey(api.getName());
	}
	
}
