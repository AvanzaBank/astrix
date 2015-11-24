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

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * A DynamicPropertyChain is a hierarchical set of properties. A property
 * is resolved to the first {@link DynamicConfigProperty} in the chain
 * that have a value set. If no property in the chain has a value then
 * the default value is used.
 * 
 * The {@link DynamicPropertyChainListener} will be notified synchronously
 * when it is registered by calling {@link #bindTo(DynamicPropertyChainListener)}.
 * After the initial notification with the current value, the listener is notified 
 * every time the resolved property changes, for instance when:
 * 
 * <ul>
 *  <li>A property with higher precedence than the current resolved property is set</li>
 *  <li>The current resolved property is changed. In that case 
 *  the listener will be notified with the new value</li>
 *  <li>The current resolved property is cleared. In that case 
 *  the listener will be notified with the new (possibly default) resolved value</li>
 * </ul>
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
final class DynamicPropertyChain<T> implements DynamicPropertyListener<DynamicConfigProperty<T>> {

	private final LinkedList<DynamicConfigProperty<T>> chain = new LinkedList<>();
	private volatile Optional<PropertyChangeEventDispatcher> propertyChainListener;
	private final PropertyParser<T> parser;
	private final T defaultValue;
	
	private DynamicPropertyChain(T defaultValue, PropertyParser<T> parser) {
		this.defaultValue = defaultValue;
		this.parser = parser;
		this.propertyChainListener = Optional.empty();
	}
	
	/**
	 * Binds the resolved value of this chain to a given listener. The listener will be
	 * notified synchronously with the current resolved value of this property chain, and
	 * will later receive a notification each time the resolved of this chain changes.
	 * 
	 */
	void bindTo(DynamicPropertyChainListener<T> l) {
		this.propertyChainListener = Optional.of(new PropertyChangeEventDispatcher(l));
		this.propertyChainListener.ifPresent(PropertyChangeEventDispatcher::init);
	}


	static <T> DynamicPropertyChain<T> createWithDefaultValue(T defaultValue, PropertyParser<T> parser) {
		return new DynamicPropertyChain<>(defaultValue, parser);
	}
	
	private T get() {
		return chain.stream()
					.filter(DynamicConfigProperty::isSet)
					.findFirst()
					.map(DynamicConfigProperty::get)
					.orElse(defaultValue);
	}

	@Override
	public void propertyChanged(DynamicConfigProperty<T> updatedProperty) {
		this.propertyChainListener.ifPresent(PropertyChangeEventDispatcher::propertyChanged);
	}
	
	DynamicConfigProperty<T> appendValue() {
		DynamicConfigProperty<T> property = DynamicConfigProperty.create(this, parser);
		chain.addLast(property);
		return property;
	}
	
	private class PropertyChangeEventDispatcher {
		private DynamicPropertyChainListener<T> listener;
		private T lastNotifiedState;
		
		public PropertyChangeEventDispatcher(DynamicPropertyChainListener<T> listener) {
			this.listener = listener;
		}

		private void init() {
			propertyChanged();
		}

		private void propertyChanged() {
			T currentResolvedValue = get();
			if (!Objects.equals(currentResolvedValue, lastNotifiedState)) {
				listener.propertyChanged(currentResolvedValue);
				lastNotifiedState = currentResolvedValue;
			}
		}
	}
	
}
