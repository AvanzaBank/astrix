/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
final class DynamicPropertyListenerSupport<T> {
	
	private final List<SubscribedListener<T>> listeners = new CopyOnWriteArrayList<>();
	
	void addListener(DynamicPropertyListener<T> l) {
		listeners.add(new SubscribedListener<>(l));
	}
	
	void notifyListeners(T newValue) {
		for (SubscribedListener<T> subscribedListener : listeners) {
			subscribedListener.notifyListener(newValue);
		}
	}
	
	void removeListener(DynamicPropertyListener<T> l) {
		listeners.removeAll(Arrays.asList(new SubscribedListener<>(l)));
	}
	
	private static class SubscribedListener<T> {
		private DynamicPropertyListener<T> listener;

		public SubscribedListener(DynamicPropertyListener<T> listener) {
			this.listener = Objects.requireNonNull(listener);
		}

		void notifyListener(T newValue) {
			this.listener.propertyChanged(newValue);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SubscribedListener)) {
				return false;
			}
			return listener == SubscribedListener.class.cast(obj).listener;
		}
		
		@Override
		public int hashCode() {
			return System.identityHashCode(listener);
		}
		
		@Override
		public String toString() {
			return "DynamicPropertyListener(" + this.listener.toString() + ")";
		}
	}

}
