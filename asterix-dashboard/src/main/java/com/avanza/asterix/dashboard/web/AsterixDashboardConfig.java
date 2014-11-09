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
package com.avanza.asterix.dashboard.web;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avanza.asterix.context.AsterixFrameworkBean;
import com.avanza.asterix.context.AsterixSettings;
import com.avanza.asterix.provider.component.AsterixServiceComponentNames;
import com.avanza.asterix.service.registry.client.AsterixServiceRegistryAdministrator;

@Configuration
public class AsterixDashboardConfig {
	
	@Bean
	public AsterixFrameworkBean asterix() {
		AsterixFrameworkBean result = new AsterixFrameworkBean();
		result.setConsumedAsterixBeans(Arrays.<Class<?>>asList(
			AsterixServiceRegistryAdministrator.class
		));
		return result;
	}
	
	@Bean
	public AsterixSettings asterixSettings() {
		return new AsterixSettings() {{
			set(ASTERIX_SERVICE_REGISTRY_URI, AsterixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=service-registry");
		}};
	}
	
}
