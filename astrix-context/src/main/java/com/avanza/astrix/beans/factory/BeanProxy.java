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
package com.avanza.astrix.beans.factory;

import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.function.Supplier;

import rx.Observable;

public interface BeanProxy {
	/**
	 * proxy an synchronous invocation. <p>
	 * 
	 * @param command
	 * @return
	 */
	<T> CheckedCommand<T> proxyInvocation(CheckedCommand<T> command);
	
	/**
	 * Proxy an asynchronous invocation. The invocation is represented by
	 * an Observable, although the underlying service method invocation can
	 * use some other representation for the asynchrounous result. 
	 * 
	 * 
	 * @param command
	 * @return
	 */
	<T> Supplier<Observable<T>> proxyAsyncInvocation(Supplier<Observable<T>> command);
	
	public static class NoProxy implements BeanProxy {

		@Override
		public <T> CheckedCommand<T> proxyInvocation(CheckedCommand<T> command) {
			return command;
		}

		@Override
		public <T> Supplier<Observable<T>> proxyAsyncInvocation(Supplier<Observable<T>> command) {
			return command;
		}

		public static BeanProxy create() {
			return new NoProxy();
		}
	}
}