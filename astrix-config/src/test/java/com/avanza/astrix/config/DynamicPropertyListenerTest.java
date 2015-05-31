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
import static org.junit.Assert.assertNull;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

public class DynamicPropertyListenerTest {
	
	@Test
	public void intPropertyListenersAreNotifiedWhenPropertySet() throws Exception {
		PropertySpy<Integer> propertySpy = new PropertySpy<>();
		DynamicIntProperty prop = new DynamicIntProperty(1);
		prop.addListener(propertySpy);
		
		propertySpy.receivesNoChange();
		prop.set(2);
		propertySpy.receivesProperyChangeWithValue(2);
	}
	
	@Test
	public void intPropertyDoesNotNotifyUnsubscribedListeners() throws Exception {
		DynamicIntProperty prop = new DynamicIntProperty(1);
		PropertySpy<Integer> propertySpy = new PropertySpy<>();
		prop.addListener(propertySpy);
		prop.set(2);
		propertySpy.receivesProperyChangeWithValue(2);

		prop.removeListener(propertySpy);
		prop.set(3);
		propertySpy.receivesNoChange();
		
	}
	
	@Test
	public void booleanPropertyListenersAreNotifiedWhenPropertySet() throws Exception {
		PropertySpy<Boolean> propertySpy = new PropertySpy<>();
		DynamicBooleanProperty prop = new DynamicBooleanProperty(false);
		prop.addListener(propertySpy);
		
		propertySpy.receivesNoChange();
		prop.set(true);
		propertySpy.receivesProperyChangeWithValue(true);
	}
	
	@Test
	public void booleanPropertyDoesNotNotifyUnsubscribedListeners() throws Exception {
		PropertySpy<Boolean> propertySpy = new PropertySpy<>();
		DynamicBooleanProperty prop = new DynamicBooleanProperty(false);
		prop.addListener(propertySpy);
		prop.set(false);
		propertySpy.receivesProperyChangeWithValue(false);
		
		prop.removeListener(propertySpy);
		prop.set(true);
		propertySpy.receivesNoChange();
	}
	
	@Test
	public void longPropertyListenersAreNotifiedWhenPropertySet() throws Exception {
		PropertySpy<Long> propertySpy = new PropertySpy<>();
		DynamicLongProperty prop = new DynamicLongProperty(1);
		prop.addListener(propertySpy);
		
		propertySpy.receivesNoChange();
		prop.set(2);
		propertySpy.receivesProperyChangeWithValue(2L);
	}
	
	@Test
	public void longPropertyDoesNotNotifyUnsubscribedListeners() throws Exception {
		PropertySpy<Long> propertySpy = new PropertySpy<>();
		DynamicLongProperty prop = new DynamicLongProperty(1);
		prop.addListener(propertySpy);
		prop.set(2);
		propertySpy.receivesProperyChangeWithValue(2L);
		
		prop.removeListener(propertySpy);
		prop.set(3);
		propertySpy.receivesNoChange();
	}
	
	@Test
	public void stringPropertyListenersAreNotifiedWhenPropertySet() throws Exception {
		PropertySpy<String> propertySpy = new PropertySpy<>();
		DynamicStringProperty prop = new DynamicStringProperty("1");
		prop.addListener(propertySpy);
		
		propertySpy.receivesNoChange();
		prop.set("2");
		propertySpy.receivesProperyChangeWithValue("2");
	}
	
	@Test
	public void stringPropertyDoesNotNotifyUnsubscribedListeners() throws Exception {
		PropertySpy<String> propertySpy = new PropertySpy<>();
		DynamicStringProperty prop = new DynamicStringProperty("1");
		prop.addListener(propertySpy);
		prop.set("2");
		propertySpy.receivesProperyChangeWithValue("2");
		
		prop.removeListener(propertySpy);
		prop.set("3");
		propertySpy.receivesNoChange();
	}
	
	private static class PropertySpy<T> implements DynamicPropertyListener<T> {
		final Queue<T> notifiedChanges = new LinkedBlockingQueue<>();

		@Override
		public void propertyChanged(T newValue) {
			notifiedChanges.add(newValue);
		}

		public void receivesProperyChangeWithValue(T expected) {
			assertEquals("Last propertyChangedEvent\n", expected, notifiedChanges.poll());
		}

		public void receivesNoChange() {
			T value = notifiedChanges.poll();
			assertNull("Expected no propertyChanged event, but event with value:\n" + value, value);
		}
		
	}
	

}
