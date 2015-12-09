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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;



public class DynamicConfigTest {
	
	
	MapConfigSource firstSource = new MapConfigSource();
	MapConfigSource secondSource = new MapConfigSource();
	DynamicConfig dynamicConfig = new DynamicConfig(Arrays.asList(firstSource, secondSource));
	
	@Test
	public void propertyIsResolvedToFirstOccurenceInConfigSources() throws Exception {
		DynamicStringProperty stringProperty = dynamicConfig.getStringProperty("foo", "defaultFoo");
		
		secondSource.set("foo", "secondValue");
		assertEquals("secondValue", stringProperty.get());
		
		firstSource.set("foo", "firstValue");
		assertEquals("firstValue", stringProperty.get());
		
		secondSource.set("foo", "secondNewValue");
		assertEquals("firstValue", stringProperty.get());
	}
	
	@Test
	public void propertyValueReturnsToDefaultValueWhenConfigSourceValueIsRemoved() throws Exception {
		DynamicStringProperty stringProperty = dynamicConfig.getStringProperty("foo", "defaultFoo");
		
		firstSource.set("foo", "firstValue");
		assertEquals("firstValue", stringProperty.get());
		
		firstSource.set("foo", null);
		assertEquals("defaultFoo", stringProperty.get());
		
	}
	
	@Test
	public void booleanProperty() throws Exception {
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", false);
		
		secondSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "false");
		assertFalse(booleanProperty.get());
	}
	
	@Test
	public void intProperty() throws Exception {
		DynamicIntProperty intProperty = dynamicConfig.getIntProperty("foo", 0);
		assertEquals(0, intProperty.get());
		
		secondSource.set("foo", "2");
		assertEquals(2, intProperty.get());
		
		firstSource.set("foo", "1");
		assertEquals(1, intProperty.get());
	}
	
	@Test
	public void unparsableBooleanPropertiesAreIgnored() throws Exception {
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", false);
		
		secondSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "true[L]");
		assertTrue(booleanProperty.get());
		
		secondSource.set("foo", "MALFORMED");
		assertTrue(booleanProperty.get());
		
		secondSource.set("foo", "false");
		assertFalse(booleanProperty.get());
		
		firstSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "MALFORMED");
		assertTrue(booleanProperty.get());
	}
	
	@Test
	public void unparsableIntPropertiesAreIgnored() throws Exception {
		DynamicIntProperty intProperty = dynamicConfig.getIntProperty("foo", 0);
		
		secondSource.set("foo", "2d");
		assertEquals(0, intProperty.get());
		
		secondSource.set("foo", "1");
		assertEquals(1, intProperty.get());
		
		firstSource.set("foo", "1s");
		assertEquals(1, intProperty.get());
	}
	
	@Test
	public void merge() throws Exception {
		MapConfigSource thirdSource = new MapConfigSource();
		DynamicConfig dynamicConfigB = new DynamicConfig(Arrays.asList(thirdSource));
		
		DynamicConfig merged = DynamicConfig.merged(dynamicConfig, dynamicConfigB);
		
		assertEquals("defaultValue", merged.getStringProperty("foo", "defaultValue").get());
		
		thirdSource.set("foo", "thirdValue");
		assertEquals("thirdValue", merged.getStringProperty("foo", "defaultValue").get());
		
		firstSource.set("foo", "firstValue");
		assertEquals("firstValue", merged.getStringProperty("foo", "defaultValue").get());
		
		secondSource.set("foo", "secondValue");
		assertEquals("firstValue", merged.getStringProperty("foo", "defaultValue").get());
	}
	
	@Test
	public void propertyListenerSupport_StringType() throws Exception {
		firstSource.set("foo", "1");
		secondSource.set("foo", "2");
		Queue<String> events = new LinkedBlockingQueue<>();
		DynamicStringProperty prop = dynamicConfig.getStringProperty("foo", "-");
		prop.addListener(events::add);
		
		assertEquals("1", prop.getCurrentValue());
		assertNull("Listener should not be notified before a property actually changes", events.poll());
		secondSource.set("foo", "22");
		assertNull("Listener should not be notfied when a property changes but resolved property has the same value", events.poll());
		
		firstSource.set("foo", "11");
		assertEquals("Listener should be notified when property resolved changes", "11", events.poll());
		
		secondSource.set("foo", "11");
		assertNull(events.poll());
	}
	
	@Test
	public void propertyListenerSupport_IntType() throws Exception {
		firstSource.set("intProp", "1");
		DynamicIntProperty prop = dynamicConfig.getIntProperty("intProp", 0);
		Queue<Integer> events = new LinkedBlockingQueue<>();
		prop.addListener(events::add);
		
		assertEquals(1, prop.getCurrentValue().intValue());
		assertNull("Listener should not be notified before a property actually changes", events.poll());

		firstSource.set("intProp", "11");
		assertEquals("Listener should be notified when property resolved changes", Integer.valueOf(11), events.poll());
	}
	
	@Test
	public void propertyListenerSupport_LongType() throws Exception {
		firstSource.set("longProp", "1");
		DynamicLongProperty prop = dynamicConfig.getLongProperty("longProp", 0);
		Queue<Long> events = new LinkedBlockingQueue<>();
		prop.addListener(events::add);
		
		assertEquals(1, prop.getCurrentValue().longValue());
		assertNull("Listener should not be notified before a property actually changes", events.poll());

		firstSource.set("longProp", "11");
		assertEquals("Listener should be notified when property resolved changes", Long.valueOf(11), events.poll());
	}
	
	@Test
	public void propertyListenerSupport_BooleanType() throws Exception {
		firstSource.set("booleanProp", "true");
		DynamicBooleanProperty prop = dynamicConfig.getBooleanProperty("booleanProp", false);
		Queue<Boolean> events = new LinkedBlockingQueue<>();
		prop.addListener(events::add);
		
		assertEquals(Boolean.TRUE, prop.getCurrentValue());
		assertNull("Listener should not be notified before a property actually changes", events.poll());

		firstSource.set("booleanProp", "false");
		assertEquals("Listener should be notified when property resolved changes", Boolean.FALSE, events.poll());
	}
	
	@Test
	public void globalPropertyListenerIsNotifiedWhenNewPropertyIsCreated() throws Exception {
		firstSource.set("booleanProp", "true");
		DynamicBooleanProperty prop = dynamicConfig.getBooleanProperty("booleanProp", false);
		Queue<Boolean> events = new LinkedBlockingQueue<>();
		prop.addListener(events::add);
		
		assertEquals(Boolean.TRUE, prop.getCurrentValue());
		assertNull("Listener should not be notified before a property actually changes", events.poll());

		firstSource.set("booleanProp", "false");
		assertEquals("Listener should be notified when property resolved changes", Boolean.FALSE, events.poll());
	}
	
	@Test
	public void globalConfigListenerSupport_PropertyCreatedEvents() throws Exception {
		class Prop {
			String name;
			Object val;
			public Prop(String name, Object val) {
				this.name = name;
				this.val = val;
			}
			@Override
			public String toString() {
				return name + "=" + val;
			}
		}
		MapConfigSource firstSource = new MapConfigSource();
		firstSource.set("foo", "1");
		MapConfigSource secondSource = new MapConfigSource();
		secondSource.set("foo", "2");
		
		DynamicConfig config = DynamicConfig.create(firstSource, secondSource);
		Queue<Prop> propertyCreatedEvents = new LinkedBlockingQueue<>();
		config.addListener(new DynamicConfigListener() {
			@Override
			public void propertyCreated(String propertyName, Object initialValue) {
				propertyCreatedEvents.add(new Prop(propertyName, initialValue));
			}
		});
		
		assertNull(propertyCreatedEvents.poll());
		config.getStringProperty("foo", "default");
		assertEquals("foo", propertyCreatedEvents.poll().name);
		
		// Should return cached value and not fire created event
		config.getStringProperty("foo", "default");
		assertNull(propertyCreatedEvents.poll());
	}
	
	@Test
	public void globalConfigListenerSupport_PropertyUpdateEvents() throws Exception {
		class Prop {
			String name;
			Object val;
			public Prop(String name, Object val) {
				this.name = name;
				this.val = val;
			}
			@Override
			public String toString() {
				return name + "=" + val;
			}
		}
		MapConfigSource firstSource = new MapConfigSource();
		firstSource.set("foo", "1");
		MapConfigSource secondSource = new MapConfigSource();
		secondSource.set("foo", "2");
		
		DynamicConfig config = DynamicConfig.create(firstSource, secondSource);
		Queue<Prop> propertyChangeEvents = new LinkedBlockingQueue<>();
		config.addListener(new DynamicConfigListener() {
			@Override
			public void propertyChanged(String propertyName, Object newValue) {
				propertyChangeEvents.add(new Prop(propertyName, newValue));
			}
		});
		
		assertNull(propertyChangeEvents.poll());
		config.getStringProperty("foo", "default");
		assertNull(propertyChangeEvents.poll());
		
		firstSource.set("foo", "11");
		Prop prop = propertyChangeEvents.poll();
		assertNotNull(prop);
		assertEquals("foo", prop.name);
		assertEquals("11", prop.val);

		secondSource.set("foo", "11");
		assertNull(propertyChangeEvents.poll());
	}
	
	

	

}
