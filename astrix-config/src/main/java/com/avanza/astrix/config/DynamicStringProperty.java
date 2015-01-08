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

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicStringProperty {
	
	private static final DynamicPropertyListener<String> NO_LISTENER = new DynamicPropertyListener<String>() {
		@Override
		public void propertyChanged(String newValue) {
		}
	};
	
	private volatile String value;
	private final DynamicPropertyListener<String> listener;
	
	public DynamicStringProperty(String initialValue) {
		this.value = initialValue;
		this.listener = NO_LISTENER;
	}
	
	public DynamicStringProperty(String initialValue, DynamicPropertyListener listener) {
		this.value = initialValue;
		this.listener = Objects.requireNonNull(listener);
	}

	public String get() {
		return this.value;
	}
	
	public void set(String value) {
		this.value = value;
		this.listener.propertyChanged(value);
	}

}
