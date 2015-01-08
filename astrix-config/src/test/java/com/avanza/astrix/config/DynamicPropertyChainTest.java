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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Test;



public class DynamicPropertyChainTest {
	
	private static final DynamicPropertyChainListener<String> DUMMY_LISTENER = new DynamicPropertyChainListener<String>() {
		@Override
		public void propertyChanged(String newValue) {
		}
	};
	
	@Test
	public void emptyChainReturnsDefaultValue() throws Exception {
		DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue("defaultFoo", DUMMY_LISTENER, new PropertyParser.StringParser());
		assertEquals("defaultFoo", propertyChain.get());
	}
	
	@Test
	public void chainReturnsDefaultValue() throws Exception {
		DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue("defaultFoo", DUMMY_LISTENER, new PropertyParser.StringParser());
		DynamicConfigProperty<String> dynamicProperty = propertyChain.prependValue();
		assertEquals("defaultFoo", propertyChain.get());
		dynamicProperty.set("Foo");
		assertEquals("Foo", dynamicProperty.get());
	}
	
	@Test
	public void chainClearsReturnsToDefaultValue() throws Exception {
		DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue("defaultFoo", DUMMY_LISTENER, new PropertyParser.StringParser());
		DynamicConfigProperty<String> dynamicProperty = propertyChain.prependValue();
		assertEquals("defaultFoo", propertyChain.get());
		dynamicProperty.set("Foo");
		assertEquals("Foo", dynamicProperty.get());
		dynamicProperty.set(null);
		assertEquals("defaultFoo", dynamicProperty.get());
	}
	
	@Test
	public void resolvesPropertyInOrder() throws Exception {
		DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue("defaultFoo", DUMMY_LISTENER, new PropertyParser.StringParser());
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.prependValue();
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.prependValue();
		
		secondPropertyInChain.set("Foo");
		assertEquals("Foo", propertyChain.get());
		firstPropertyInChain.set("Bar");
		assertEquals("Bar", propertyChain.get());
		secondPropertyInChain.set("Foo2");
		assertEquals("Bar", propertyChain.get());
	}
	
	@Test
	public void notifiesListener() throws Exception {
		final Queue<String> propertyChanges = new LinkedList<>();
		DynamicPropertyChain<String> propertyChain = DynamicPropertyChain.createWithDefaultValue("defaultFoo", new DynamicPropertyChainListener<String>() {
			@Override
			public void propertyChanged(String newValue) {
				propertyChanges.add(newValue);
			}
		}, new PropertyParser.StringParser());
		
		DynamicConfigProperty<String> secondPropertyInChain = propertyChain.prependValue();
		DynamicConfigProperty<String> firstPropertyInChain = propertyChain.prependValue();
		
		secondPropertyInChain.set("Foo");
		assertEquals("Foo", propertyChanges.poll());
		firstPropertyInChain.set("Bar");
		assertEquals("Bar", propertyChanges.poll());
		
		secondPropertyInChain.set("Foo2");
		assertEquals(null, propertyChanges.poll());
		assertTrue(propertyChanges.isEmpty());
	}
	

}
