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


/**
 * DynamicProperty of String type, see {@link DynamicProperty}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicStringProperty implements DynamicProperty<String> {

	private final ListenerSupport<DynamicPropertyListener<String>> listenerSupport = new ListenerSupport<>();
	private volatile String value;
	
	public DynamicStringProperty(String initialValue) {
		this.value = initialValue;
	}
	
	public DynamicStringProperty() {
		this.value = null;
	}
	
	@Override
	public String getCurrentValue() {
		return this.value;
	}
	
	public String get() {
		return this.value;
	}
	
	public void set(String value) {
		this.value = value;
		this.listenerSupport.dispatchEvent(l -> l.propertyChanged(value));
	}
	
	@Override
	public void setValue(String value) {
		set(value);
	}
	
	@Override
	public String toString() {
		return this.value;
	}

	@Override
	public void addListener(DynamicPropertyListener<String> listener) {
		listenerSupport.addListener(listener);
	}

	@Override
	public void removeListener(DynamicPropertyListener<String> listener) {
		listenerSupport.removeListener(listener);
	}
	
}
