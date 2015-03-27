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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.core.AstrixCallStackTrace;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * @author Elias Lindholm (elilin)
 */
public class HystrixCommandFacade<T> {

	private static final Logger log = LoggerFactory.getLogger(HystrixCommandFacade.class);

	private final Command<T> command;
	private final CommandSettings settings;
	
	private HystrixCommandFacade(Command<T> command, CommandSettings settings) {
		this.command = command;
		this.settings = settings;
	}

	public static <T> T execute(Command<T> command, CommandSettings settings) {
		return new HystrixCommandFacade<>(command, settings).doExecute();
	}

	private T doExecute() {
		HystrixCommand<HystrixResult<T>> command = createHystrixCommand();
		HystrixResult<T> result = command.execute();
		throwExceptionIfExecutionFailed(result);
		return result.getResult();
	}

	private void throwExceptionIfExecutionFailed(HystrixResult<T> result) {
		if (result.getException() != null) {
			AstrixCallStackTrace trace = new AstrixCallStackTrace();
			appendStackTrace(result.getException(), trace);
			if (result.getException() instanceof RuntimeException) {
				throw (RuntimeException) result.getException();
			}
			throw new RuntimeException("Command execution failed: " + result.getException());
		}
	}

	private void appendStackTrace(Throwable exception, AstrixCallStackTrace trace) {
		Throwable lastThowableInChain = exception;
		while (lastThowableInChain.getCause() != null) {
			lastThowableInChain = lastThowableInChain.getCause();
		}
		lastThowableInChain.initCause(trace);
	}

	private HystrixCommand<HystrixResult<T>> createHystrixCommand() {
		return new HystrixCommand<HystrixResult<T>>(createHystrixConfiguration()) {

			@Override
			protected HystrixResult<T> run() throws Exception {
				try {
					return HystrixResult.success(command.call());
				} catch (Exception e) {
					return handleException(e);
				} 
			}

			private HystrixResult<T> handleException(Exception cause) {
				if (cause instanceof ServiceUnavailableException) {
					// Only ServiceUnavailableExceptions are propagated and counted as failures for the circuit breaker
					throw (ServiceUnavailableException) cause;
				}
				// Any other exception is treated as a service exception and does not count as failures for the circuit breaker
				return HystrixResult.exception(cause);
			}

			@Override
			protected HystrixResult<T> getFallback() {
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

	private Setter createHystrixConfiguration() {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties.Setter()
						.withExecutionTimeoutInMilliseconds(settings.getExecutionIsolationThreadTimeoutInMilliseconds());
						
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter()
						.withMaxQueueSize(settings.getMaxQueueSize())
						.withQueueSizeRejectionThreshold(settings.getQueueSizeRejectionThreshold())
						.withCoreSize(settings.getCoreSize());

		return Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(settings.getGroupKey()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(settings.getCommandKey()))
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private static class HystrixResult<T> {

		private T result;
		private Throwable exception;

		public static <T> HystrixResult<T> success(T result) {
			return new HystrixResult<>(result, null);
		}

		public static <T> HystrixResult<T> exception(Throwable exception) {
			return new HystrixResult<>(null, exception);
		}

		public HystrixResult(T result, Throwable exception) {
			this.result = result;
			this.exception = exception;
		}

		public T getResult() {
			return result;
		}

		public Throwable getException() {
			return exception;
		}

	}
}
