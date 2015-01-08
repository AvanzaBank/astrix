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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;



public class DynamicConfigTest {
	
	
	@Test
	public void propertyIsResolvedByFirstOccurence() throws Exception {
		MapConfigSource firstSource = new MapConfigSource();
		MapConfigSource secondSource = new MapConfigSource();
		DynamicConfig dynamicConfig = new DynamicConfig(Arrays.asList(firstSource, secondSource));
		
		DynamicStringProperty stringProperty = dynamicConfig.getStringProperty("foo", "defaultFoo");
		
		secondSource.set("foo", "secondValue");
		assertEquals("secondValue", stringProperty.get());
		
		firstSource.set("foo", "firstValue");
		assertEquals("firstValue", stringProperty.get());
		
		secondSource.set("foo", "secondNewValue");
		assertEquals("firstValue", stringProperty.get());
	}
	
	@Test
	public void booleanProperty() throws Exception {
		MapConfigSource firstSource = new MapConfigSource();
		MapConfigSource secondSource = new MapConfigSource();
		DynamicConfig dynamicConfig = new DynamicConfig(Arrays.asList(firstSource, secondSource));
		
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", false);
		
		secondSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "false");
		assertFalse(booleanProperty.get());
	}
	
	@Test
	public void unparsablePropertiesAreIgnored() throws Exception {
		MapConfigSource firstSource = new MapConfigSource();
		MapConfigSource secondSource = new MapConfigSource();
		DynamicConfig dynamicConfig = new DynamicConfig(Arrays.asList(firstSource, secondSource));
		
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", false);
		
		secondSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "true[L]");
		assertTrue(booleanProperty.get());
		
		secondSource.set("foo", "MALFORMED");
		assertTrue(booleanProperty.get());
	}
	
	@Test
	public void ifAllPropertiesAreUnparsableItFallbacksToDefaultValue() throws Exception {
		MapConfigSource firstSource = new MapConfigSource();
		MapConfigSource secondSource = new MapConfigSource();
		secondSource.set("foo", "true[2]");
		firstSource.set("foo", "true[L]");
		DynamicConfig dynamicConfig = new DynamicConfig(Arrays.asList(firstSource, secondSource));
		
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", true);
		assertTrue(booleanProperty.get());
	}
	
	private static class MapConfigSource implements DynamicConfigSource {
		
		private ConcurrentMap<String, ListenabeStringProperty> propertyValues = new ConcurrentHashMap<>();
		
		@Override
		public String get(String propertyName, DynamicPropertyListener propertyChangeListener) {
			ListenabeStringProperty dynamicProperty = getProperty(propertyName);
			dynamicProperty.listeners.add(propertyChangeListener);
			return dynamicProperty.value;
		}
		
		public void set(String propertyName, String value) {
			getProperty(propertyName).set(value);
		}

		private ListenabeStringProperty getProperty(String propertyName) {
			propertyValues.putIfAbsent(propertyName, new ListenabeStringProperty());
			ListenabeStringProperty dynamicProperty = propertyValues.get(propertyName);
			return dynamicProperty;
		}
		
		
	}
	
	static class ListenabeStringProperty {
		
		private final List<DynamicPropertyListener> listeners = new LinkedList<>();
		private volatile String value;
		
		void propertyChanged(String newValue) {
			for (DynamicPropertyListener l : listeners) {
				l.propertyChanged(newValue);
			}
		}

		void set(String value) {
			this.value = value;
			propertyChanged(value);
		}
	}
	

}
