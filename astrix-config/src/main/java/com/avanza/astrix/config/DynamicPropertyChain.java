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


public class DynamicPropertyChain implements DynamicPropertyListener {
	
	private volatile DynamicConfigProperty chain;
	private DynamicPropertyChainListener propertyListener;
	
	public DynamicPropertyChain(DynamicConfigProperty chain, DynamicPropertyChainListener propertyListener) {
		this.chain = chain;
		this.propertyListener = propertyListener;
	}

	public static DynamicPropertyChain createWithDefaultValue(String defaultValue, DynamicPropertyChainListener listener) {
		return new DynamicPropertyChain(DynamicConfigProperty.terminal(defaultValue, new DynamicPropertyListener() {
			@Override
			public void propertyChanged(String newValue) {
			}
		}), listener);
	}
	
	public String get() {
		return chain.get();
	}

	public DynamicConfigProperty prependValue() {
		chain = DynamicConfigProperty.chained(chain, this);
		return chain;
	}

	@Override
	public void propertyChanged(String newValue) {
		String resolvedValue = get();
		if (Objects.equals(newValue, resolvedValue)) {
			// Resolved value was updated, fire property change
			propertyListener.propertyChanged(get());
		}
	}
	
}
