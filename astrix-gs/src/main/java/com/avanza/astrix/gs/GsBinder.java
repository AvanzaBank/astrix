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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;
import com.j_spaces.core.client.SpaceURL;

/**
 * 
 * @author Elias Lindholm
 *
 */
public class GsBinder implements AstrixConfigAware {
	
	public static final String SPACE_NAME_PROPERTY = "spaceName";
	public static final String SPACE_URL_PROPERTY = "spaceUrl";
	
	private static final Pattern SPACE_URL_PATTERN = Pattern.compile("jini://.*?/.*?/(.*)?[?](.*)");
	private DynamicConfig config;
	
	public GigaSpace getEmbeddedSpace(ApplicationContext applicationContext) {
		String optionalGigaSpaceBeanName = config.getStringProperty(AstrixSettings.GIGA_SPACE_BEAN_NAME, null).get();
		if (optionalGigaSpaceBeanName  != null) {
			return applicationContext.getBean(optionalGigaSpaceBeanName, GigaSpace.class);
		}
		return findEmbeddedSpace(applicationContext);
	}

	private GigaSpace findEmbeddedSpace(ApplicationContext applicationContext) {
		GigaSpace result = null;
		for (GigaSpace gigaSpace : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, GigaSpace.class).values()) {
			if (isEmbedded(gigaSpace)) {
				if (result != null) {
					throw new IllegalStateException("Multiple embedded spaces defined in applicationContext");
				} else {
					result = gigaSpace;
				}
			}
		}
		if (result == null) {
			throw new IllegalStateException("Failed to find an embedded space in applicationContext");
		}
		return result;
	}

	private boolean isEmbedded(GigaSpace gigaSpace) {
		try {
			return gigaSpace.getSpace().isEmbedded();
		} catch (Exception e) {
			// Clearly not an embedded space since they are not stateful
			return false;
		}
	}
	
	public AstrixServiceProperties createProperties(GigaSpace space) {
		AstrixServiceProperties result = new AstrixServiceProperties();
		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, space.getSpace().getName());
		result.setProperty(SPACE_URL_PROPERTY, new SpaceUrlBuilder(space).buildSpaceUrl());
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
		private String versioned;
		
		public SpaceUrlBuilder(GigaSpace space) {
			SpaceURL finderURL = space.getSpace().getFinderURL();
			this.locators = finderURL.getProperty("locators");
			this.groups = finderURL.getProperty("groups");
			this.versioned = Boolean.toString(space.getSpace().isOptimisticLockingEnabled());
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
			result.append("&versioned=").append(this.versioned);
			return result.toString();
		}
		
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}

}
