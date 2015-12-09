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
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Test;



public class DynamicPropertyChainTest {
	
	
	private static final String DEFAULT_VALUE = "defaultFoo";
	
	Queue<String> propertyChanges = new LinkedList<>();
	DynamicPropertyChainListener<String> listener = propertyChanges::add;
	DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue(DEFAULT_VALUE, new PropertyParser.StringParser());
	
	@Test
	public void listenerIsNotifiedWithDefaultValueWhenRegisteredIfNoPropertiesExistsInChain() throws Exception {
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
	}
	
	@Test
	public void listenerIsRegisteredWithNewValueWhenDynamicPropertyIsSet() throws Exception {
		DynamicConfigProperty<String> dynamicProperty = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		dynamicProperty.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
	}
	
	@Test
	public void chainReturnsToDefaultValueWhenAllPropertiesInChainAreNull() throws Exception {
		DynamicConfigProperty<String> dynamicProperty = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		
		dynamicProperty.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
		
		dynamicProperty.set(null);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
	}
	
	@Test
	public void resolvesPropertyInOrder() throws Exception {
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.appendValue();
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		secondPropertyInChain.set("Foo");
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		
		secondPropertyInChain.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
		
		firstPropertyInChain.set("Bar");
		assertEquals("Bar", propertyChanges.poll());
		
		secondPropertyInChain.set("Foo2");
		assertEquals(null, propertyChanges.poll());
	}
	
	@Test
	public void notifiesListener() throws Exception {
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.appendValue();
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		
		secondPropertyInChain.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
		firstPropertyInChain.set("Bar");
		assertEquals("Bar", propertyChanges.poll());
		
		secondPropertyInChain.set("Foo2");
		assertEquals(null, propertyChanges.poll());
		assertTrue(propertyChanges.isEmpty());
	}
	
	@Test
	public void listenerShouldOnlyBeNotifiedWhenUnderlyingPropertyActuallyChanges() throws Exception {
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.appendValue();
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		
		secondPropertyInChain.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
		firstPropertyInChain.set("Bar");
		assertEquals("Bar", propertyChanges.poll());
		
		secondPropertyInChain.set("Bar");
		assertEquals(null, propertyChanges.poll());
		assertTrue(propertyChanges.isEmpty());
	}
	
	@Test
	public void listenerShouldBeNotifiedWithNewValueWhenCurrentResolvedConfigurationPropertyIsClearedAndPropertyWithLowerPrecedenceExists() throws Exception {
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.appendValue();
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.appendValue();
		propertyChain.bindTo(listener);
		assertEquals(DEFAULT_VALUE, propertyChanges.poll());
		
		// Setting second property should trigger event with new value
		secondPropertyInChain.set("2");
		assertEquals("2", propertyChanges.poll());
		
		// Setting first property should trigger event with new value
		firstPropertyInChain.set("1");
		assertEquals("1", propertyChanges.poll());
		
		// Clearing first property should trigger event with value from second prop
		firstPropertyInChain.set(null);
		assertEquals("2", propertyChanges.poll());
	}
	
	@Test
	public void getsNotifiedAboutResolvedValueWhenSetBeforeRegistereingListener() throws Exception {
		DynamicConfigProperty<String> propertyInChain = propertyChain.appendValue();
		propertyInChain.set("2");
		
		propertyChain.bindTo(propertyChanges::add);
		assertEquals("2", propertyChanges.poll());
	}
	

}
