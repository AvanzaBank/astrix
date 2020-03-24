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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import com.avanza.astrix.core.function.CheckedCommand;

public class ContextPropagation {

    private final List<ContextPropagator> propagators;

    public static ContextPropagation create(List<ContextPropagator> propagators) {
        return new ContextPropagation(propagators);
    }

    public ContextPropagation(List<ContextPropagator> propagators) {
        this.propagators = Collections.unmodifiableList(new ArrayList<>(propagators));
    }

    public <T> CheckedCommand<T> wrap(CheckedCommand<T> call) {
        return wrap(call, ContextPropagator::wrap);
    }

    public Runnable wrap(Runnable c) {
        return wrap(c, ContextPropagator::wrap);
    }

    public <T> T wrap(T operation, BiFunction<ContextPropagator, T, T> wrapper) {
        T wrapping = operation;
        for (ContextPropagator propagator : propagators) {
            wrapping = wrapper.apply(propagator, wrapping);
        }
        return wrapping;
    }

}
