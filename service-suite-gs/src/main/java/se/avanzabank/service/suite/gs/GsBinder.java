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
package se.avanzabank.service.suite.gs;

import se.avanzabank.service.suite.bus.client.AstrixServiceProperties;
import se.avanzabank.space.UsesLookupGroupsSpaceLocator;
import se.avanzabank.space.UsesLookupLocatorsSpaceLocator;


public class GsBinder {
	
	public GsFactory createGsFactory(AstrixServiceProperties properties, String targetSpace) {
		String lookupType = properties.getProperty("lookupType");
		String lookupName = properties.getProperty("lookupName");
		if (lookupType.equals("locators")) {
			return new GsFactory(new UsesLookupLocatorsSpaceLocator(lookupName), targetSpace);
		}
		if (lookupType.equals("groups")) {
			return new GsFactory(new UsesLookupGroupsSpaceLocator(lookupName), targetSpace);
		}
		throw new IllegalArgumentException("Cannot create GSFactory from properties: " + properties);
	}

}
