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

import java.util.LinkedList;
import java.util.Objects;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class DynamicPropertyChain<T> implements DynamicPropertyListener<T> {

	private final LinkedList<DynamicConfigProperty<T>> chain = new LinkedList<>();
	private final DynamicPropertyChainListener<T> propertyListener;
	private final PropertyParser<T> parser;
	private final T defaultValue;
	
	public DynamicPropertyChain(T defaultValue, DynamicPropertyChainListener<T> propertyListener, PropertyParser<T> parser) {
		this.defaultValue = defaultValue;
		this.propertyListener = propertyListener;
		this.parser = parser;
	}

	public static <T> DynamicPropertyChain<T> createWithDefaultValue(T defaultValue, DynamicPropertyChainListener<T> listener, PropertyParser<T> parser) {
		return new DynamicPropertyChain<>(defaultValue, listener, parser);
	}
	
	public T get() {
		for (DynamicConfigProperty<T> prop : chain) {
			T value = prop.get();
			if (value != null) {
				return value;
			}
		}
		return defaultValue;
	}

	public DynamicConfigProperty<T> prependValue() {
		DynamicConfigProperty<T> property = DynamicConfigProperty.chained(this, parser);
		chain.addFirst(property);
		return property;
	}

	@Override
	public void propertyChanged(T newValue) {
		T resolvedValue = get();
		if (Objects.equals(newValue, resolvedValue)) {
			// Resolved value was updated, fire property change
			propertyListener.propertyChanged(get());
		}
	}
	
}
