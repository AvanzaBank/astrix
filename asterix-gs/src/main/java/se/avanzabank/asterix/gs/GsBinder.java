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

import org.openspaces.core.GigaSpace;

import se.avanzabank.asterix.context.AsterixServiceProperties;


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
		result.setQualifier(space.getSpace().getName()); // TODO: note that gigaSpace.getName returns "GigaSpace" on embedded space
		result.setProperty(SPACE_NAME_PROPERTY, space.getSpace().getName()); // TODO: note that gigaSpace.getName returns "GigaSpace" on embedded space
		result.setProperty(SPACE_URL_PROPERTY, space.getSpace().getFinderURL().getURL());
		return result;
	}

}
