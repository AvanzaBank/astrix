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
package com.avanza.astrix.gs;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;

import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.j_spaces.core.IJSpace;



public class GsBinderTest {
	
	private AstrixSettings settings = new AstrixSettings();
	
	@Test
	public void parsesASpaceUrl() throws Exception {
		GsBinder gsBinder = new GsBinder();
		gsBinder.setSettings(AstrixSettingsReader.create(settings));
		AstrixServiceProperties serviceProperties = gsBinder.createServiceProperties("jini://*/*/service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se");
		assertEquals("service-registry-space", serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY));
		assertEquals("service-registry-space", serviceProperties.getQualifier());
		assertEquals("jini://*/*/service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se", serviceProperties.getProperty(GsBinder.SPACE_URL_PROPERTY));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionForInvalidSpaceUrls() throws Exception {
		GsBinder gsBinder = new GsBinder();
		gsBinder.setSettings(AstrixSettingsReader.create(settings));
		gsBinder.createServiceProperties("jini://service-registry-space?locators=testgssystem01.test.aza.se,testgssystem02.test.aza.se");
	}

}
