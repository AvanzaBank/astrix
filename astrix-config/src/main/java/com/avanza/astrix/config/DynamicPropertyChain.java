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

import java.util.Objects;


public class DynamicPropertyChain<T> implements DynamicPropertyListener<T> {
	
	private volatile DynamicConfigProperty<T> chain;
	private DynamicPropertyChainListener<T> propertyListener;
	private PropertyParser<T> parser;
	
	public DynamicPropertyChain(DynamicConfigProperty<T> chain, DynamicPropertyChainListener<T> propertyListener, PropertyParser<T> parser) {
		this.chain = chain;
		this.propertyListener = propertyListener;
		this.parser = parser;
	}

	public static <T> DynamicPropertyChain<T> createWithDefaultValue(T defaultValue, DynamicPropertyChainListener<T> listener, PropertyParser<T> parser) {
		return new DynamicPropertyChain<>(DynamicConfigProperty.terminal(defaultValue, new DynamicPropertyListener<T>() {
			@Override
			public void propertyChanged(T newValue) {
			}
		}, parser), listener, parser);
	}
	
	public T get() {
		return chain.get();
	}

	public DynamicConfigProperty<T> prependValue() {
		chain = DynamicConfigProperty.chained(chain, this, parser);
		return chain;
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
