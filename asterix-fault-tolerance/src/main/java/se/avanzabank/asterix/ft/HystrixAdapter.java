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
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.core.ServiceUnavailableException;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

public class HystrixAdapter<T> implements InvocationHandler {

	private T provider;
	private Class<T> api;
	private String group;
	private HystrixCommandSettings settings;

	private static final Logger log = LoggerFactory.getLogger(HystrixAdapter.class);
	
	public HystrixAdapter(Class<T> api, T provider, String group) {
		this(api, provider, group, new HystrixCommandSettings());
	}
	
	public HystrixAdapter(Class<T> api, T provider, String group, HystrixCommandSettings settings) {
		this.api = api;
		this.provider = provider;
		this.group = group;
		this.settings = Objects.requireNonNull(settings);
	}

	public static <T> T create(Class<T> api, T provider, String group, HystrixCommandSettings settings) {
		log.debug("Adding fault tolerance: api={}, group={}", api, group);
		if (!api.isInterface()) {
			throw new IllegalArgumentException(
					"Can only add fault tolerance to an api exposed using an interface. Exposed api=" + api);
		}
		return api.cast(Proxy.newProxyInstance(HystrixAdapter.class.getClassLoader(), new Class[] { api },
				new HystrixAdapter<T>(api, provider, group, settings)));
	}
	
	public static <T> T create(Class<T> api, T provider, String group) {
		return create(api, provider, group, new HystrixCommandSettings());
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		/*
		 * TODO: inspect method an use correct isolation strategy:
		 * 
		 *   1. Use HystrixObservableCommand with semaphore isolation for non-blocking operations (methods returning Observable's/Futures)
		 *   2. Use HystrixCommand with thread isolation for all blocking operations (any method NOT returning an Observable/Future)
		 */
		HystrixResult result = createHystrixCommand(method, args).execute();
		if (result.getException() != null) {
			throw result.getException();
		}
		return result.getResult();
	}

	private Exception handleException(RuntimeException e) throws Throwable {
		Throwable cause = e.getCause();
		if (cause instanceof InvocationTargetException) {
			InvocationTargetException ex = (InvocationTargetException) e.getCause();
			// TODO handle null cause
			throw ex.getCause();
		}
		throw cause;
	}

	
	private HystrixCommand<HystrixResult> createHystrixCommand(final Method method, final Object[] args) {
		return new HystrixCommand<HystrixResult>(getKeys()) {
			
			@Override
			protected HystrixResult run() throws Exception {
				try {
					return HystrixResult.success(method.invoke(provider, args));
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (cause instanceof ServiceUnavailableException) {
						throw (ServiceUnavailableException)cause;
					}
					return HystrixResult.exception(e.getCause());
				}
				catch (Exception e) {
					return HystrixResult.exception(e);
				}
			}

			@Override
			protected HystrixResult getFallback() {
				return HystrixResult.exception(new ServiceUnavailableException());
			}
			
			
		};
	}
	
	private static class HystrixResult {
		
		private Object result;
		private Throwable exception;
		
		public static HystrixResult success(Object result) {
			return new HystrixResult(result, null);
		}
		
		public static HystrixResult exception(Throwable exception) {
			return new HystrixResult(null, exception);
		}
		
		public HystrixResult(Object result, Throwable exception) {
			this.result = result;
			this.exception = exception;
		}
		public Object getResult() {
			return result;
		}

		public Throwable getException() {
			return exception;
		}
		
	}

	private Setter getKeys() {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(
						settings.getExecutionIsolationThreadTimeoutInMilliseconds());
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter().withMaxQueueSize(1_000_000)
						.withQueueSizeRejectionThreshold(settings.getQueueSizeRejectionThreshold()).withCoreSize(settings.getCoreSize());

		return Setter.withGroupKey(getGroupKey())
				.andCommandKey(getCommandKey())
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private HystrixCommandKey getCommandKey() {
		if (settings.getCommandKey() == null) {
			return HystrixCommandKey.Factory.asKey(api.getSimpleName());
		}
		return HystrixCommandKey.Factory.asKey(settings.getCommandKey());
	}

	private HystrixCommandGroupKey getGroupKey() {
		return HystrixCommandGroupKey.Factory.asKey(group);
	}
}
