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
package com.avanza.astrix.beans.tracing;

import com.avanza.astrix.beans.ft.ContextPropagator;

public class MyContextPropagator implements ContextPropagator {

    //private static final AtomicInteger count = new AtomicInteger(0);

    @Override
    public <T> ThrowingCallable<T> wrap(ThrowingCallable<T> call) {
        //int num = count.incrementAndGet();
        //System.out.println(this.getClass().getSimpleName() + " was here! Call#=" + num);
        return call;
//                () -> {
//            System.out.println("Executing Call#=" + num);
//            return call.call();
//        };
    }
}
