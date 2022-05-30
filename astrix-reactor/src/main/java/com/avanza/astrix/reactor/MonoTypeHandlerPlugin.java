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
package com.avanza.astrix.reactor;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.One;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class MonoTypeHandlerPlugin implements ReactiveTypeHandlerPlugin<Mono<Object>> {

    @Override
    public Observable<Object> toObservable(Mono<Object> reactiveType) {
        ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
        reactiveType.subscribe(subject::onNext, subject::onError, subject::onCompleted);
        return subject;
    }

    @Override
    public Mono<Object> toReactiveType(Observable<Object> observable) {
        One<Object> sink = Sinks.one();
        observable.subscribe(sink::tryEmitValue, sink::tryEmitError, sink::tryEmitEmpty);
        return sink.asMono();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Mono<Object>> reactiveTypeHandled() {
        Class<?> type = Mono.class;
        return (Class<Mono<Object>>) type;
    }
}
