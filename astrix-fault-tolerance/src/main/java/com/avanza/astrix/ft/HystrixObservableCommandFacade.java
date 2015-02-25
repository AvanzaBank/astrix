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

import rx.Observable;
import rx.functions.Func1;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * @author Elias Lindholm (elilin)
 */
public class HystrixObservableCommandFacade<T> {

	public static <T> Observable<T> observe(final Observable<T> observable, ObservableCommandSettings settings) {
		Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(settings.getGroupKey()))
							  .andCommandKey(HystrixCommandKey.Factory.asKey(settings.getCommandKey()))
							  .andCommandPropertiesDefaults(com.netflix.hystrix.HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(settings.getTimeoutMillis())
									  																			 .withExecutionIsolationSemaphoreMaxConcurrentRequests(settings.getMaxConcurrentRequests()));
		
		Observable<Result<T>> faultToleranceProtectedObservable = new HystrixObservableCommand<Result<T>>(setter) {
			@Override
			protected Observable<Result<T>> construct() {
				return observable.map(new Func1<T, Result<T>>() {
					@Override
					public Result<T> call(T t1) {
						return Result.success(t1);
					}
				}).onErrorResumeNext(new Func1<Throwable, Observable<? extends Result<T>>>() {
					@Override
					public Observable<? extends Result<T>> call(Throwable t1) {
						if (t1 instanceof ServiceUnavailableException) {
							return Observable.error(t1);
						}
						// Wrap all exception thrown by underlying observable
						// in Result.exception. This will by-pass hystrix
						// circuit-breaker logic and not count as a failure.
						// I.e we don't want the circuit-breaker to open 
						// due to exceptions thrown by the underlying service.
						return Observable.just(Result.<T>exception(t1));
					}
				});
			}
			
			@Override
			protected Observable<Result<T>> resumeWithFallback() {
				/* 
				 * This method will will be invoked in any of this circumstances:
				 *  - Underlying observable did not emit event before timeout, "service timeout"
				 *  - Circuit Breaker rejected subscription to underlying observable, "circuit open"
				 *  - Bulk Head rejected subscription to underlying observable, "to many outstanding requests"
				 *  - Underlying observable threw ServiceUnavailableException
				 *  
				 *  Either way, just return a ServiceUnavailableException.
				 * 
				 */
				return Observable.just(Result.<T>exception(new ServiceUnavailableException()));
			}
		}.observe(); // Eagerly start execution of underlying observable
		return faultToleranceProtectedObservable.flatMap(new Func1<Result<T>, Observable<T>>() {
			@Override
			public Observable<T> call(Result<T> t1) {
				return t1.toObservable();
			}
		}).onErrorResumeNext(new Func1<Throwable, Observable<? extends T>>() {
			@Override
			public Observable<? extends T> call(Throwable t1) {
				if (t1 instanceof HystrixRuntimeException) {
					HystrixRuntimeException e = (HystrixRuntimeException) t1;
					return Observable.error(e.getCause());
				}
				return Observable.error(t1);
			}
		});
	}
	
	private static class Result<T> {
		private final T value;
		private final Throwable exception;
		
		public Result(T value, Throwable exception) {
			this.value = value;
			this.exception = exception;
		}

		public static <T> Result<T> success(T result) {
			return new Result<T>(result, null);
		}
		
		/**
		 * @param throwable
		 * @return
		 */
		public static <T> Result<T> exception(Throwable throwable) {
			return new Result<T>(null, throwable);
		}
		
		public Observable<T> toObservable() {
			if (this.value != null) {
				return Observable.just(this.value);
			}
			return Observable.error(this.exception);
		}
		
		
	}
	

}
