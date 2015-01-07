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
package com.avanza.astrix.config.archauis;

import static org.junit.Assert.*;

import org.junit.Test;

import com.avanza.astrix.config.Config;
import com.avanza.astrix.config.StringProperty;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;



public class ArchauisConfigAdapterTest {
	
	@Test
	public void testName() throws Exception {
		
		DynamicConfiguration archauisConfig = new DynamicConfiguration();
		DynamicPropertyFactory archaiusPropertyFactory = DynamicPropertyFactory.initWithConfigurationSource(archauisConfig);
		
		ArchauisConfigAdapter archauisConfigSource = new ArchauisConfigAdapter(archaiusPropertyFactory);
		Config config = new Config(archauisConfigSource);
		
		StringProperty fooProp = config.getStringProperty("foo", "defaultFoo");
		assertEquals("defaultFoo", fooProp.get());
		
		archauisConfig.setProperty("foo", "fooValue");
		assertEquals("fooValue", fooProp.get());

		archauisConfig.setProperty("foo", "fooNewValue");
		assertEquals("fooNewValue", fooProp.get());
		
	}

}
