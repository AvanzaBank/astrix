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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.core.GigaSpace;
import org.springframework.context.ApplicationContext;

import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.j_spaces.core.client.SpaceURL;

/**
 * 
 * @author Elias Lindholm
 *
 */
public class GsBinder implements AstrixSettingsAware {
	
	public static final String SPACE_NAME_PROPERTY = "spaceName";
	public static final String SPACE_URL_PROPERTY = "spaceUrl";
	
	private static final Pattern SPACE_URL_PATTERN = Pattern.compile("jini://.*?/.*?/(.*)?[?](.*)");
	private AstrixSettingsReader settings;
	
	public GsFactory createGsFactory(AstrixServiceProperties properties) {
		String spaceUrl = properties.getProperty(SPACE_URL_PROPERTY);
		return new GsFactory(spaceUrl);
	}
	
	public GigaSpace getEmbeddedSpace(ApplicationContext applicationContext) {
		String optionalGigaSpaceBeanName = settings.getString(AstrixSettings.GIGA_SPACE_BEAN_NAME, null);
		if (optionalGigaSpaceBeanName  != null) {
			return applicationContext.getBean(optionalGigaSpaceBeanName, GigaSpace.class);
		}
		return findEmbeddedSpace(applicationContext);
	}

	private GigaSpace findEmbeddedSpace(ApplicationContext applicationContext) {
		GigaSpace result = null;
		for (GigaSpace gigaSpace : applicationContext.getBeansOfType(GigaSpace.class).values()) {
			if (gigaSpace.getSpace().isEmbedded()) {
				if (result != null) {
					throw new IllegalStateException("Multiple embedded spaces defined in applicationContext");
				} else {
					result = gigaSpace;
				}
			}
		}
		return result;
	}
	
	public AstrixServiceProperties createProperties(GigaSpace space) {
		AstrixServiceProperties result = new AstrixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, space.getSpace().getName());
		result.setProperty(SPACE_URL_PROPERTY, new SpaceUrlBuilder(space).buildSpaceUrl());
		result.setQualifier(space.getSpace().getName());
		return result;
	}

	public AstrixServiceProperties createServiceProperties(String spaceUrl) {
		Matcher spaceUrlMatcher = SPACE_URL_PATTERN.matcher(spaceUrl);
		if (!spaceUrlMatcher.find()) {
			throw new IllegalArgumentException("Invalid spaceUrl: " + spaceUrl);
		}
		String spaceName = spaceUrlMatcher.group(1);
		AstrixServiceProperties result = new AstrixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, spaceName);
		result.setProperty(SPACE_URL_PROPERTY, spaceUrl);
		result.setQualifier(spaceName);
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

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}

}
