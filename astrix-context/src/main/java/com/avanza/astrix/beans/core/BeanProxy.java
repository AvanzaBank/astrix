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

import java.util.function.Supplier;

import com.avanza.astrix.core.function.CheckedCommand;

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
	 * Proxy a reactive invocation. The invocation is represented by
	 * an Observable, although the underlying service method invocation can
	 * use some other representation for the reactive result. 
	 * 
	 * 
	 * @param command
	 * 
	 * @return A Proxied Supplier for the underlying invocation. Note
	 * that the underlying (argument) supplier MUST be called synchronously
	 * when invoking "get" on the returned Supplier.
	 */
	<T> Supplier<Observable<T>> proxyReactiveInvocation(Supplier<Observable<T>> command);
	
	String name();
	
	boolean isEnabled();
	
	public static class NoProxy implements BeanProxy {

		@Override
		public <T> CheckedCommand<T> proxyInvocation(CheckedCommand<T> command) {
			return command;
		}

		@Override
		public <T> Supplier<Observable<T>> proxyReactiveInvocation(Supplier<Observable<T>> command) {
			return command;
		}

		public static BeanProxy create() {
			return new NoProxy();
		}
		
		@Override
		public String name() {
			return "noProxy";
		}

		@Override
		public boolean isEnabled() {
			return false;
		}
	}
}