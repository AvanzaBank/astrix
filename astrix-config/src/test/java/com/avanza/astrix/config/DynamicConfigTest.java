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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

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
	public void booleanProperty() throws Exception {
		DynamicBooleanProperty booleanProperty = dynamicConfig.getBooleanProperty("foo", false);
		
		secondSource.set("foo", "true");
		assertTrue(booleanProperty.get());
		
		firstSource.set("foo", "false");
		assertFalse(booleanProperty.get());
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
	}
	
	@Test
	public void merge() throws Exception {
		MapConfigSource firstSource = new MapConfigSource();
		MapConfigSource secondSource = new MapConfigSource();
		MapConfigSource thirdSource = new MapConfigSource();
		DynamicConfig dynamicConfigA = new DynamicConfig(Arrays.asList(firstSource, secondSource));
		DynamicConfig dynamicConfigB = new DynamicConfig(Arrays.asList(thirdSource));
		
		DynamicConfig merged = DynamicConfig.merged(dynamicConfigA, dynamicConfigB);
		
		assertEquals("defaultValue", merged.getStringProperty("foo", "defaultValue").get());
		
		thirdSource.set("foo", "thirdValue");
		assertEquals("thirdValue", merged.getStringProperty("foo", "defaultValue").get());
		
		firstSource.set("foo", "firstValue");
		assertEquals("firstValue", merged.getStringProperty("foo", "defaultValue").get());
		
		secondSource.set("foo", "secondValue");
		assertEquals("firstValue", merged.getStringProperty("foo", "defaultValue").get());

	}
	

}
