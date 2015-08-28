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
package com.avanza.astrix.context.core;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import com.avanza.astrix.beans.core.FutureAdapter;

import rx.Observable;

public final class AsyncTypeConverterImpl implements AsyncTypeConverter {
	
	private final ConcurrentMap<Class<?>, AsyncTypeConverterPlugin> pluginByAsyncType = new ConcurrentHashMap<>();
	
	public AsyncTypeConverterImpl(List<AsyncTypeConverterPlugin> typeConverterPlugins) {
		for (AsyncTypeConverterPlugin asyncTypeConverterPlugin : typeConverterPlugins) {
			this.pluginByAsyncType.put(asyncTypeConverterPlugin.asyncType(), asyncTypeConverterPlugin);
		}
		this.pluginByAsyncType.put(Future.class, new FutureTypeConverter());
	}

	@Override
	public Observable<Object> toObservable(Class<?> fromType, Object asyncTypeInstance) {
		return getPlugin(fromType).toObservable(asyncTypeInstance);
	}

	@Override
	public Object toAsyncType(Class<?> targetType, Observable<Object> observable) {
		return getPlugin(targetType).fromObservable(observable);
	}
	
	private AsyncTypeConverterPlugin getPlugin(Class<?> type) {
		AsyncTypeConverterPlugin plugin = this.pluginByAsyncType.get(type);
		if (plugin == null) {
			throw new IllegalArgumentException("Cant convert async type to rx.Observable. No adapter registered for async type: " + type);
		}
		return plugin;
	}
	
	@Override
	public boolean canAdaptToType(Class<?> type) {
		return pluginByAsyncType.containsKey(type);
	}
		
	
	private static class FutureTypeConverter implements AsyncTypeConverterPlugin {
		
		@Override
		public Observable<Object> toObservable(Object asyncResult) {
			return Observable.from((Future<?>) asyncResult);
		}
		
		@Override
		public Object fromObservable(Observable<Object> asyncResult) {
			return new FutureAdapter<>(asyncResult);
		}


		@Override
		public Class<?> asyncType() {
			return Future.class;
		}
		
	}
	
}
