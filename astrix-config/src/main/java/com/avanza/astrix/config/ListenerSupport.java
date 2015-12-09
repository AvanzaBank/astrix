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
package com.avanza.astrix.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
final class ListenerSupport<T> {
	
	private final List<SubscribedListener> listeners = new CopyOnWriteArrayList<>();
	
	
	void addListener(T l) {
		listeners.add(new SubscribedListener(l));
	}
	
	void dispatchEvent(Consumer<T> eventNotification) {
		for (SubscribedListener subscribedListener : listeners) {
			eventNotification.accept(subscribedListener.listener);
		}
	}
	
	void removeListener(T l) {
		listeners.removeAll(Arrays.asList(new SubscribedListener(l)));
	}
	
	private class SubscribedListener {
		private T listener;

		public SubscribedListener(T l) {
			this.listener = Objects.requireNonNull(l);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ListenerSupport.SubscribedListener)) {
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
			return "DynamicConfigListener(" + this.listener.toString() + ")";
		}
	}

}
