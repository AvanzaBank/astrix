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
package com.avanza.astrix.gs;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.SettableFuture;
import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * 
 * @author Elias Lindholm
 *
 */
public class AsyncFutureTypeHandler implements ReactiveTypeHandlerPlugin<AsyncFuture<Object>> {

	@Override
	public Observable<Object> toObservable(AsyncFuture<Object> reactiveType) {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		reactiveType.setListener(result -> {
			Exception exception = result.getException();
			if(exception == null) {
				subject.onNext(result.getResult());
				subject.onCompleted();
			} else {
				subject.onError(exception);
			}
		});
		return subject;
	}

	@Override
	public AsyncFuture<Object> toReactiveType(Observable<Object> observable) {
		SettableFuture<Object> reactiveType = new SettableFuture<>();
		observable.subscribe(reactiveType::setResult, reactiveType::setResult);
		return reactiveType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<AsyncFuture<Object>> reactiveTypeHandled() {
		Class<?> type = AsyncFuture.class;
		return (Class<AsyncFuture<Object>>) type;
	}

}
