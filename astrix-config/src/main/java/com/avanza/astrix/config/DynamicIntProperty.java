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
 * DynamicProperty of int type, see {@link DynamicProperty}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicIntProperty implements DynamicProperty<Integer> {

	private final ListenerSupport<DynamicPropertyListener<Integer>> listenerSupport = new ListenerSupport<>();
	private volatile int value;
	
	public DynamicIntProperty() {
	}
	
	public DynamicIntProperty(int initialValue) {
		this.value = initialValue;
	}
	
	@Override
	public Integer getCurrentValue() {
		return value;
	}
	
	public int get() {
		return value;
	}
	
	public void set(int value) {
		this.value = value;
		this.listenerSupport.dispatchEvent(l -> l.propertyChanged(value));
	}
	
	@Override
	public void setValue(Integer value) {
		set(value);
	}
	
	@Override
	public String toString() {
		return Integer.toString(value);
	}
	
	@Override
	public void addListener(DynamicPropertyListener<Integer> listener) {
		listenerSupport.addListener(listener);
	}

	@Override
	public void removeListener(DynamicPropertyListener<Integer> listener) {
		listenerSupport.removeListener(listener);
	}
	
}
