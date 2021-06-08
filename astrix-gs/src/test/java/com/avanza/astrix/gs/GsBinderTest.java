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
package com.avanza.astrix.gs;

import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.ConfigSource;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class GsBinderTest {
	
	private final ConfigSource config = new MapConfigSource();
	
	@Test
	void parsesASpaceUrl() {
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		ServiceProperties serviceProperties = gsBinder.createServiceProperties("jini://*/*/service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se");
		assertEquals("service-registry-space", serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY));
		assertEquals("service-registry-space", serviceProperties.getQualifier());
		assertEquals("jini://*/*/service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se", serviceProperties.getProperty(GsBinder.SPACE_URL_PROPERTY));
	}
	
	@Test
	void throwsIllegalArgumentExceptionForInvalidSpaceUrls() {
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		assertThrows(IllegalArgumentException.class, () -> gsBinder.createServiceProperties("jini://service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se"));
	}

}
