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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.avanza.astrix.core.function.CheckedCommand;

/**
 * Contains a list of {@link ContextPropagator}s that may be applied to supplied
 * actions. See {@link ContextPropagator} for further description of wrapping
 * operations.
 */
public class ContextPropagation {

    public static final ContextPropagation NONE = new ContextPropagation(Collections.emptyList());

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

    public <T> Consumer<T> wrap(Consumer<T> c) {
        // We transform the supplied Consumer into a Runnable by using a local
        // reference to the parameter value passed into the Consumer. This will
        // not work if the Consumer is called simultaneously by different
        // threads. But in our use-cases in Astrix, this is never the case.
        // The reason we want to transform the Consumer into a Runnable is so
        // that we can reuse the wrapping operations that operate on Runnable.
        final AtomicReference<T> parameter = new AtomicReference<>();
        final Runnable wrappedRunnable = wrap(() -> {
            c.accept(parameter.get());
        });
        return value -> {
            parameter.set(value);
            wrappedRunnable.run();
        };
    }
}
