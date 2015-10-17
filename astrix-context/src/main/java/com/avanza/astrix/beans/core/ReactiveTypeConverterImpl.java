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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import rx.Observable;

public final class ReactiveTypeConverterImpl implements ReactiveTypeConverter {
	
	private final ConcurrentMap<Class<?>, ReactiveTypeHandlerPlugin<?>> pluginByReactiveType = new ConcurrentHashMap<>();
	
	public ReactiveTypeConverterImpl(List<ReactiveTypeHandlerPlugin<?>> typeConverterPlugins) {
		for (ReactiveTypeHandlerPlugin<?> asyncTypeConverterPlugin : typeConverterPlugins) {
			this.pluginByReactiveType.put(asyncTypeConverterPlugin.reactiveTypeHandled(), asyncTypeConverterPlugin);
		}
		this.pluginByReactiveType.putIfAbsent(CompletableFuture.class, new CompletableFutureTypeHandlerPlugin());
	}

	@Override
	public <T> Observable<Object> toObservable(Class<T> fromType, T reactiveType) {
		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(fromType);
		return Observable.create((s) -> {
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
		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(targetType);
		T reactiveType = plugin.newReactiveType();
		// Eagerly subscribe to the given observable
		observable.subscribe((next) -> plugin.complete(next, reactiveType), (error) -> plugin.completeExceptionally(error, reactiveType));
		return reactiveType;
	}
	
	private <T> ReactiveTypeHandlerPlugin<T> getPlugin(Class<T> type) {
		ReactiveTypeHandlerPlugin<T> plugin = (ReactiveTypeHandlerPlugin<T>) this.pluginByReactiveType.get(type);
		if (plugin == null) {
			throw new IllegalArgumentException("Cant convert reactive type to rx.Observable. No ReactiveTypeHandlerPlugin registered for type: " + type);
		}
		return plugin;
	}
	
	@Override
	public boolean isReactiveType(Class<?> type) {
		return pluginByReactiveType.containsKey(type);
	}		
	
}
