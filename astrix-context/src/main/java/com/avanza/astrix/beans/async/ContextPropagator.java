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
package com.avanza.astrix.beans.async;

import com.avanza.astrix.core.function.CheckedCommand;

/**
 * Defines actions that may be applied to an operation before it is executed.
 * For example, this might be a decoration of the call state for a particular
 * function, or logging of a certain call.
 * In Astrix, this is used to carry along thread state when switching execution
 * to a different thread. The state of the calling thread can be retrieved when
 * {@link #wrap} is called, and later restored on another thread by the
 * operation returned by the wrapping operation.
 */
public interface ContextPropagator {

    <T> CheckedCommand<T> wrap(CheckedCommand<T> call);
    Runnable wrap(Runnable runnable);

}
