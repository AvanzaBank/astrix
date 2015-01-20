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
package com.avanza.astrix.service.registry.client;

import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;

@AstrixApiProvider
public class AstrixServiceRegistryLibraryProvider implements AstrixSettingsAware {
	
	private AstrixSettingsReader settings;

	@Library
	public AstrixServiceRegistryClient createClient(AstrixServiceRegistry serviceRegistry) {
		return new AstrixServiceRegistryClientImpl(serviceRegistry, settings.getString(AstrixSettings.SUBSYSTEM_NAME));
	}
	
	@Library
	public AstrixServiceRegistryAdministrator createAdministrator(AstrixServiceRegistry serviceRegistry) {
		return new AstrixServiceRegistryAdministratorImpl(serviceRegistry);
	}
	
	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}

}
