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
package se.avanzabank.asterix.ft;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;

public class HystrixAdapter<T> implements InvocationHandler {

	private static final int DEFAULT_THREAD_TIMEOUT_MILLIS = 1000;
	private T provider;
	private Class<T> api;
	private String group;

	private static final Logger log = LoggerFactory.getLogger(HystrixAdapter.class);
	
	public HystrixAdapter(Class<T> api, T provider, String group) {
		this.api = api;
		this.provider = provider;
		this.group = group;
	}

	public static <T> T create(Class<T> api, T provider, String group) {
		log.debug("Adding fault tolerance, api=" + api + ", provider class=" + provider.getClass() + " group=" + group);
		if (!api.isInterface()) {
			throw new IllegalArgumentException(
					"Can only add fault tolerance to an api exposed using an interface. Exposed api=" + api);
		}
		return api.cast(Proxy.newProxyInstance(HystrixAdapter.class.getClassLoader(), new Class[] { api },
				new HystrixAdapter<T>(api, provider, group)));
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
			return new HystrixCommand<Object>(getKeys()) {
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

	private Setter getKeys() {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(
						DEFAULT_THREAD_TIMEOUT_MILLIS);
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter().withMaxQueueSize(1_000_000)
						.withQueueSizeRejectionThreshold(10);

		return Setter.withGroupKey(getGroupKey())
				.andCommandKey(getCommandKey())
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private HystrixCommandKey getCommandKey() {
		return HystrixCommandKey.Factory.asKey(api.getSimpleName());
	}

	private HystrixCommandGroupKey getGroupKey() {
		return HystrixCommandGroupKey.Factory.asKey(group);
	}
}
