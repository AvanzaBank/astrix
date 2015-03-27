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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.context.IsolationStrategy;
import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * @author Kristoffer Erlandsson (krierl)
 * @author Elias Lindholm (elilin)
 */
public class HystrixAdapter<T> implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(HystrixAdapter.class);

	private final T provider;
	private final Class<T> api;
	private final String group;
	private final Setter hystrixConfiguration;
	private final IsolationStrategy isolationStrategy;


	public HystrixAdapter(FaultToleranceSpecification<T> spec, T provider) {
		this(spec, provider, new HystrixCommandSettings());
	}

	private HystrixAdapter(FaultToleranceSpecification<T> spec, T provider, HystrixCommandSettings settings) {
		this.api = spec.getApi();
		this.provider = provider;
		this.group = spec.getGroup();
		this.isolationStrategy = spec.getIsolationStrategy();
		this.hystrixConfiguration = getHystrixConfiguration(settings);
	}

	public static <T> T create(FaultToleranceSpecification<T> spec, T provider, HystrixCommandSettings settings) {
		Objects.requireNonNull(spec);
		Objects.requireNonNull(settings);
		Class<T> api = spec.getApi();
		log.debug("Adding fault tolerance: api={}, group={}", api, spec.getGroup());
		if (!api.isInterface()) {
			throw new IllegalArgumentException(
					"Can only add fault tolerance to an api exposed using an interface. Exposed api=" + api);
		}
		return api.cast(Proxy.newProxyInstance(HystrixAdapter.class.getClassLoader(), new Class[] { api },
				new HystrixAdapter<T>(spec, provider, settings)));
	}

	public static <T> T create(FaultToleranceSpecification<T> spec, T provider) {
		return create(spec, provider, new HystrixCommandSettings());
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		HystrixCommand<HystrixResult> command = createHystrixCommand(method, args);
		HystrixResult result = command.execute();
		throwExceptionIfExecutionFailed(result);
		return result.getResult();
	}

	private void throwExceptionIfExecutionFailed(HystrixResult result) throws Throwable {
		if (result.getException() != null) {
			AstrixCallStackTrace trace = new AstrixCallStackTrace();
			appendStackTrace(result.getException(), trace);
			throw result.getException();
		}
	}

	private void appendStackTrace(Throwable exception, AstrixCallStackTrace trace) {
		Throwable lastThowableInChain = exception;
		while (lastThowableInChain.getCause() != null) {
			lastThowableInChain = lastThowableInChain.getCause();
		}
		lastThowableInChain.initCause(trace);
	}

	private HystrixCommand<HystrixResult> createHystrixCommand(final Method method, final Object[] args) {
		return new HystrixCommand<HystrixResult>(hystrixConfiguration) {

			@Override
			protected HystrixResult run() throws Exception {
				try {
					return HystrixResult.success(method.invoke(provider, args));
				} catch (InvocationTargetException e) {
					return handleInvocationTargetException(e);
				} catch (Exception e) {
					// This would be a programming error
					return HystrixResult.exception(e);
				}
			}

			private HystrixResult handleInvocationTargetException(InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof ServiceUnavailableException) {
					// Only ServiceUnavailableExceptions are propagated and counted as failures for the circuit breaker
					throw (ServiceUnavailableException) cause;
				}
				// Any other exception is treated as a service exception and does not count as failures for the circuit breaker
				if (cause == null) {
					// Javadoc says that the cause can be null. Unclear if it ever happens with InvocationTargetExceptions, but let's be safe
					return HystrixResult.exception(e);
				}
				return HystrixResult.exception(cause);
			}

			@Override
			protected HystrixResult getFallback() {
				// getFallback is only invoked when the underlying api threw an ServiceUnavailableException, or the
				// when the invocation reached timeout. In any case, treat this as service unavailable.
				ServiceUnavailableCause cause = AstrixUtil.resolveUnavailableCause(this);
				log.info(String.format("Aborted command execution: cause=%s circuit=%s", cause, this.getCommandKey().name()));
				if (isFailedExecution()) {
					// Underlying service threw ServiceUnavailableException
					return HystrixResult.exception(getFailedExecutionException());
				}
				// Timeout or rejected in queue
				return HystrixResult.exception(new ServiceUnavailableException(Objects.toString(cause)));
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

	private Setter getHystrixConfiguration(HystrixCommandSettings settings) {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties
						.Setter()
						.withExecutionIsolationThreadTimeoutInMilliseconds(
								settings.getExecutionIsolationThreadTimeoutInMilliseconds())
						.withMetricsRollingStatisticalWindowInMilliseconds(
								settings.getMetricsRollingStatisticalWindowInMilliseconds())
						.withExecutionIsolationSemaphoreMaxConcurrentRequests(settings.getSemaphoreMaxConcurrentRequests())
						.withExecutionIsolationStrategy(determineExecutionIsolationStrategy());
						
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter().withMaxQueueSize(settings.getMaxQueueSize())
						.withQueueSizeRejectionThreshold(settings.getQueueSizeRejectionThreshold())
						.withCoreSize(settings.getCoreSize());

		return Setter.withGroupKey(getGroupKey())
				.andCommandKey(getCommandKey(settings))
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private ExecutionIsolationStrategy determineExecutionIsolationStrategy() {
		if (isolationStrategy == IsolationStrategy.THREAD) {
			return ExecutionIsolationStrategy.THREAD; 
		}
		return ExecutionIsolationStrategy.SEMAPHORE;
	}

	private HystrixCommandKey getCommandKey(HystrixCommandSettings settings) {
		if (settings.getCommandKey() == null) {
			// Circuit breakers are per command (disregarding group). Hence we append the group name
			// to ensure we do not use the same circuit breaker for different groups.
			return HystrixCommandKey.Factory.asKey(group + "_" + api.getSimpleName());
		}
		return HystrixCommandKey.Factory.asKey(settings.getCommandKey());
	}

	private HystrixCommandGroupKey getGroupKey() {
		return HystrixCommandGroupKey.Factory.asKey(group);
	}
}
