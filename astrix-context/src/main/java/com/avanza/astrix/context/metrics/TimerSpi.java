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
package com.avanza.astrix.context.metrics;

import java.util.function.Supplier;

import com.avanza.astrix.core.function.CheckedCommand;

import rx.Observable;

public interface TimerSpi {
	
	<T> CheckedCommand<T> timeExecution(CheckedCommand<T> execution);
	
	<T> Supplier<Observable<T>> timeObservable(Supplier<Observable<T>> observableFactory);

	TimerSnaphot getSnapshot();
	
	public static class NoTimer implements TimerSpi {
		@Override
		public <T> Supplier<Observable<T>> timeObservable(Supplier<Observable<T>> observableFactory) {
			return observableFactory;
		}
		
		@Override
		public <T> CheckedCommand<T> timeExecution(CheckedCommand<T> execution) {
			return execution;
		}
		
		@Override
		public TimerSnaphot getSnapshot() {
			return TimerSnaphot.empty();
		}
		
	}
}
