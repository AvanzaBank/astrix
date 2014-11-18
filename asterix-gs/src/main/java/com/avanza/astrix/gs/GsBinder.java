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

import org.openspaces.core.GigaSpace;

import com.avanza.astrix.context.AsterixServiceProperties;
import com.j_spaces.core.client.SpaceURL;


public class GsBinder {
	
	public static final String SPACE_NAME_PROPERTY = "spaceName";
	public static final String SPACE_URL_PROPERTY = "spaceUrl";
	
	public static GsFactory createGsFactory(AsterixServiceProperties properties) {
		String spaceUrl = properties.getProperty(SPACE_URL_PROPERTY);
		return new GsFactory(spaceUrl);
	}
	
	public static AsterixServiceProperties createProperties(GigaSpace space) {
		AsterixServiceProperties result = new AsterixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, space.getSpace().getName());
		result.setProperty(SPACE_URL_PROPERTY, new SpaceUrlBuilder(space).buildSpaceUrl());
		return result;
	}

	public static AsterixServiceProperties createServiceProperties(String spaceUrl) {
		String spaceName = spaceUrl.split("/")[4]; // format: jini://*/*/space-name/... 
		AsterixServiceProperties result = new AsterixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, spaceName);
		result.setProperty(SPACE_URL_PROPERTY, spaceUrl);
		return result;
	}
	
	private static class SpaceUrlBuilder {
		private String locators;
		private String groups;
		private String spaceName;
		
		public SpaceUrlBuilder(GigaSpace space) {
			SpaceURL finderURL = space.getSpace().getFinderURL();
			this.locators = finderURL.getProperty("locators");
			this.groups = finderURL.getProperty("groups");
			this.spaceName = finderURL.getSpaceName();
		}

		public String buildSpaceUrl() {
			StringBuilder result = new StringBuilder();
			result.append("jini://*/*/");
			result.append(spaceName);
			result.append("?");
			if (locators != null) {
				result.append("locators=");
				result.append(locators);
			} else {
				result.append("groups=");
				result.append(groups);
			}
			return result.toString();
		}
		
	}

}
