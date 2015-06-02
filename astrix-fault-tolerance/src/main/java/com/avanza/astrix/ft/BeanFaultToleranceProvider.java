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
package com.avanza.astrix.ft;

import rx.Observable;

import com.avanza.astrix.core.function.Supplier;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface BeanFaultToleranceProvider {
	<T> Observable<T> observe(final Supplier<Observable<T>> observableFactory, Setter settings);
	<T> T execute(CheckedCommand<T> command, com.netflix.hystrix.HystrixCommand.Setter settings) throws Throwable;
}