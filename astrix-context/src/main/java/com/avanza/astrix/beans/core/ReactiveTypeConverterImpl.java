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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReactiveTypeConverterImpl implements ReactiveTypeConverter {

	private static final List<ReactiveTypeHandlerPlugin<?>> wellKnownTypes = List.of(
			new CompletableFutureTypeHandlerPlugin(),
			new RxCompletableTypeHandlerPlugin(),
			new RxSingleTypeHandlerPlugin()
	);

	private final Map<Class<?>, ReactiveTypeHandlerPlugin<?>> pluginByReactiveType;

	public ReactiveTypeConverterImpl(List<ReactiveTypeHandlerPlugin<?>> typeConverterPlugins) {
		Map<Class<?>, ReactiveTypeHandlerPlugin<?>> pluginByReactiveType = new LinkedHashMap<>(typeConverterPlugins.size() + wellKnownTypes.size());
		for (ReactiveTypeHandlerPlugin<?> asyncTypeConverterPlugin : typeConverterPlugins) {
			pluginByReactiveType.put(asyncTypeConverterPlugin.reactiveTypeHandled(), asyncTypeConverterPlugin);
		}
		wellKnownTypes.forEach(it -> pluginByReactiveType.putIfAbsent(it.reactiveTypeHandled(), it));
		this.pluginByReactiveType = Map.copyOf(pluginByReactiveType);
	}

	@Override
	public <T> Observable<Object> toObservable(Class<T> fromType, T reactiveType) {
		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(fromType);
		return plugin.toObservable(reactiveType);
	}

	@Override
	public <T> T toCustomReactiveType(Class<T> targetType, Observable<Object> observable) {
		ReactiveTypeHandlerPlugin<T> plugin = getPlugin(targetType);
		return plugin.toReactiveType(observable);
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
		return pluginByReactiveType.containsKey(type);
	}

}
