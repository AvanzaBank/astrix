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


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class DynamicConfigProperty implements DynamicPropertyListener {
	
	private DynamicPropertyListener propertyChangeListener;
	private volatile String value;
	
	private DynamicConfigProperty(DynamicPropertyListener propertyChangeListener) {
		this.propertyChangeListener = propertyChangeListener;
	}
	
	private DynamicConfigProperty(DynamicPropertyListener propertyChangeListener, String value) {
		this.propertyChangeListener = propertyChangeListener;
		this.value = value;
	}
	
	public String get() {
		return this.value;
	}
	
	public final void set(String value) {
		this.value = value;
		propertyChangeListener.propertyChanged(value);
	}
	
	@Override
	public void propertyChanged(String newValue) {
		this.value = newValue;
		propertyChangeListener.propertyChanged(newValue);
	}
	
	public static DynamicConfigProperty chained(DynamicConfigProperty next, DynamicPropertyListener propertyChangeListener) {
		return new ChainedElement(next, propertyChangeListener);
	}
	
	public static DynamicConfigProperty terminal(String value, DynamicPropertyListener listener) {
		return new TerminalValue(value, listener);
	}
	
	static class ChainedElement extends DynamicConfigProperty {	
		private final DynamicConfigProperty next;
		
		public ChainedElement(DynamicConfigProperty next, DynamicPropertyListener propertyChangeListener) {
			super(propertyChangeListener);
			this.next = next;
		}

		public String get() {
			String result = super.get();
			if (result != null) {
				return result;
			}
			return next.get();
		}

	}
	
	static class TerminalValue extends DynamicConfigProperty {
		
		public TerminalValue(String value, DynamicPropertyListener propertyChangeListener) {
			super(propertyChangeListener, value);
		}
		
	}

}
