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

import com.avanza.astrix.context.core.AsyncTypeConverterPlugin;
import com.avanza.astrix.remoting.util.GsUtil;
import com.gigaspaces.async.AsyncFuture;

import rx.Observable;
/**
 * 
 * @author Elias Lindholm
 *
 */
public class GsAsyncFutureTypeAdapter implements AsyncTypeConverterPlugin {

	@SuppressWarnings("unchecked")
	@Override
	public Observable<Object> toObservable(Object asyncResult) {
		return GsUtil.toObservable((AsyncFuture<Object>) asyncResult);
	}

	@Override
	public Object fromObservable(Observable<Object> asyncResult) {
		return GsUtil.toAsyncFuture(asyncResult);
	}

	@Override
	public Class<?> asyncType() {
		return AsyncFuture.class;
	}

}
