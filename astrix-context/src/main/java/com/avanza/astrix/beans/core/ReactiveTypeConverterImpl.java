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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import rx.Completable;
import rx.Observable;
import rx.Single;

public final class ReactiveTypeConverterImpl implements ReactiveTypeConverter {

	private final Map<Class<?>, RxCodec<?>> rxCodecs = Map.of(
			Single.class, rxCodec(Single::toObservable, Observable::toSingle),
			Completable.class, rxCodec(Completable::toObservable, Observable::toCompletable)
	);

	private final Map<Class<?>, ReactiveTypeHandlerPlugin<?>> pluginByReactiveType;

	public ReactiveTypeConverterImpl(List<ReactiveTypeHandlerPlugin<?>> typeConverterPlugins) {
		Map<Class<?>, ReactiveTypeHandlerPlugin<?>> pluginByReactiveType = new LinkedHashMap<>(typeConverterPlugins.size() + 1);
		for (ReactiveTypeHandlerPlugin<?> asyncTypeConverterPlugin : typeConverterPlugins) {
			pluginByReactiveType.put(asyncTypeConverterPlugin.reactiveTypeHandled(), asyncTypeConverterPlugin);
		}
		pluginByReactiveType.putIfAbsent(CompletableFuture.class, new CompletableFutureTypeHandlerPlugin());
		this.pluginByReactiveType = Map.copyOf(pluginByReactiveType);
	}

	@Override
	public <T> Observable<Object> toObservable(Class<T> fromType, T reactiveType) {
		RxCodec<T> rxCodec = getRxCodec(fromType);
		if (rxCodec != null) {
			return rxCodec.toObservable(reactiveType);
		}

		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(fromType);
		return Observable.unsafeCreate((s) -> {
			plugin.subscribe(new ReactiveExecutionListener() {
				@Override
				public void onResult(Object result) {
					s.onNext(result);
					s.onCompleted();
				}
				@Override
				public void onError(Throwable t) {
					s.onError(t);
				}
			}, reactiveType);
		});
	}

	@Override
	public <T> T toCustomReactiveType(Class<T> targetType, Observable<Object> observable) {
		RxCodec<T> rxCodec = getRxCodec(targetType);
		if (rxCodec != null) {
			return rxCodec.toRxType(observable);
		}

		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(targetType);
		T reactiveType = plugin.newReactiveType();
		// Eagerly subscribe to the given observable
		observable.subscribe((next) -> plugin.complete(next, reactiveType), (error) -> plugin.completeExceptionally(error, reactiveType));
		return reactiveType;
	}

	private <T> RxCodec<T> getRxCodec(Class<T> rxType) {
		@SuppressWarnings("unchecked")
		RxCodec<T> rxCodec = (RxCodec<T>) rxCodecs.get(rxType);
		return rxCodec;
	}

	private <T> ReactiveTypeHandlerPlugin<T> getPlugin(Class<T> type) {
		@SuppressWarnings("unchecked")
		ReactiveTypeHandlerPlugin<T> plugin = (ReactiveTypeHandlerPlugin<T>) this.pluginByReactiveType.get(type);
		if (plugin == null) {
			throw new IllegalArgumentException("Cant convert reactive type to rx.Observable. No ReactiveTypeHandlerPlugin registered for type: " + type);
		}
		return plugin;
	}
	
	@Override
	public boolean isReactiveType(Class<?> type) {
		return rxCodecs.containsKey(type) || pluginByReactiveType.containsKey(type);
	}

	private static <T> RxCodec<T> rxCodec(Function<T, Observable<Object>> toObservable, Function<Observable<Object>, T> toType) {
		return new RxCodec<>() {
			@Override
			public Observable<Object> toObservable(T rxTypeInstance) {
				return toObservable.apply(rxTypeInstance);
			}

			@Override
			public T toRxType(Observable<Object> observable) {
				return toType.apply(observable);
			}
		};
	}

	private interface RxCodec <T> {

		Observable<Object> toObservable(T rxTypeInstance);

		T toRxType(Observable<Object> observable);

	}

}
