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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class DynamicConfigProperty<T> implements DynamicPropertyListener<String> {
	
	private final Logger logger = LoggerFactory.getLogger(DynamicConfigProperty.class);
	private DynamicPropertyListener<T> propertyChangeListener;
	private volatile T value;
	private final PropertyParser<T> parser;
	
	private DynamicConfigProperty(DynamicPropertyListener<T> propertyChangeListener, PropertyParser<T> propertyParser) {
		this.propertyChangeListener = propertyChangeListener;
		this.parser = propertyParser;
	}
	
	private DynamicConfigProperty(DynamicPropertyListener<T> propertyChangeListener, T value, PropertyParser<T> propertyParser) {
		this.propertyChangeListener = propertyChangeListener;
		this.value = value;
		this.parser = propertyParser;
	}
	
	public T get() {
		return this.value;
	}
	
	public final void set(String value) {
		try {
			if (value != null) {
				this.value = parser.parse(value);
			} else {
				this.value = null;
			}
			propertyChangeListener.propertyChanged(this.value);
		} catch (Exception e) {
			logger.error("Failed to parse: " + value, e);
		}
	}
	
	@Override
	public void propertyChanged(String newValue) {
		set(newValue);
	}
	
	public static <T> DynamicConfigProperty<T> chained(DynamicConfigProperty<T> next, DynamicPropertyListener<T> propertyChangeListener, PropertyParser<T> propretyParser) {
		return new ChainedElement<T>(next, propertyChangeListener, propretyParser);
	}
	
	public static <T> DynamicConfigProperty<T> terminal(T value, DynamicPropertyListener<T> listener, PropertyParser<T> propertyParser) {
		return new TerminalValue<T>(value, listener, propertyParser);
	}
	
	static class ChainedElement<T> extends DynamicConfigProperty<T> {	
		private final DynamicConfigProperty<T> next;
		
		public ChainedElement(DynamicConfigProperty<T> next, DynamicPropertyListener<T> propertyChangeListener, PropertyParser<T> propertyParser) {
			super(propertyChangeListener, propertyParser);
			this.next = next;
		}

		public T get() {
			T result = super.get();
			if (result != null) {
				return result;
			}
			return next.get();
		}

	}
	
	static class TerminalValue<T> extends DynamicConfigProperty<T> {
		
		public TerminalValue(T value, DynamicPropertyListener<T> propertyChangeListener, PropertyParser<T> propertyParser) {
			super(propertyChangeListener, value, propertyParser);
		}
		
	}

}
