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

import rx.Observable;
import rx.subjects.ReplaySubject;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureTypeHandlerPlugin implements ReactiveTypeHandlerPlugin<CompletableFuture<Object>> {

	@Override
	public Observable<Object> toObservable(CompletableFuture<Object> reactiveType) {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		reactiveType.whenComplete((result, throwable) -> {
			if (throwable == null) {
				subject.onNext(result);
				subject.onCompleted();
			} else {
				subject.onError(throwable);
			}
		});
		return subject;
	}

	@Override
	public CompletableFuture<Object> toReactiveType(Observable<Object> observable) {
		CompletableFuture<Object> reactiveType = new CompletableFuture<>();
		observable.subscribe(reactiveType::complete, reactiveType::completeExceptionally);
		return reactiveType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<CompletableFuture<Object>> reactiveTypeHandled() {
		Class<?> result = CompletableFuture.class;
		return (Class<CompletableFuture<Object>>) result;
	}
}