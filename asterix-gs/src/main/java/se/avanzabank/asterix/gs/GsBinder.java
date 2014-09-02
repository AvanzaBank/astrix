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
package se.avanzabank.asterix.gs;

import net.jini.core.discovery.LookupLocator;

import org.openspaces.core.GigaSpace;

import se.avanzabank.asterix.bus.client.AsterixServiceProperties;
import se.avanzabank.space.UsesLookupGroupsSpaceLocator;
import se.avanzabank.space.UsesLookupLocatorsSpaceLocator;

import com.j_spaces.core.client.SpaceURL;


public class GsBinder {
	
	public static GsFactory createGsFactory(AsterixServiceProperties properties, String targetSpace) {
//		String lookupType = properties.getProperty("lookupType");
//		String lookupType = properties.getProperty("lookupType");
		String locators = properties.getProperty("locators");
		String groups = properties.getProperty("groups");
		if (locators != null) {
			return new GsFactory(new UsesLookupLocatorsSpaceLocator(locators), targetSpace);
		}
		if (groups != null) {
			return new GsFactory(new UsesLookupGroupsSpaceLocator(groups), targetSpace);
		}
		throw new IllegalArgumentException("Cannot create GSFactory from properties: " + properties);
	}
	
	public static AsterixServiceProperties createProperties(GigaSpace space) {
		AsterixServiceProperties result = new AsterixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setQualifier(space.getSpace().getName()); // TODO: note that gigaSpace.getName returns "GigaSpace" on embedded space
		SpaceURL finderURL = space.getSpace().getFinderURL();
		LookupLocator[] locators = finderURL.getLookupLocators();
		if (locators != null) {
			StringBuilder locatorsString = new StringBuilder();
			for (LookupLocator locator : locators) {
				if (locatorsString.length() > 0) {
					locatorsString.append(",");
					
				}
				// TODO: how to convert locator to string?
				locatorsString.append(locator.getHost());
			}
			result.setProperty("locators", locatorsString.toString()); 
		} else {
			StringBuilder groupsString = new StringBuilder();
			for (String group : finderURL.getLookupGroups()) {
				if (groupsString.length() > 0) {
					groupsString.append(",");
					
				}
				groupsString.append(group);
			}
			result.setProperty("groups", groupsString.toString());
		}
		return result;
	}

}
