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

import com.avanza.astrix.context.AsterixSettings;
import com.avanza.astrix.context.AsterixSettingsAware;
import com.avanza.astrix.context.AsterixSettingsReader;
import com.avanza.astrix.provider.library.AsterixExport;
import com.avanza.astrix.provider.library.AsterixLibraryProvider;

@AsterixLibraryProvider
public class AsterixServiceRegistryLibrary implements AsterixSettingsAware {
	
	private AsterixSettingsReader settings;

	@AsterixExport
	public AsterixServiceRegistryClient createClient(AsterixServiceRegistry serviceRegistry) {
		return new AsterixServiceRegistryClientImpl(serviceRegistry, settings.getString(AsterixSettings.SUBSYSTEM_NAME));
	}
	
	@AsterixExport
	public AsterixServiceRegistryAdministrator createAdministrator(AsterixServiceRegistry serviceRegistry) {
		return new AsterixServiceRegistryAdministratorImpl(serviceRegistry);
	}
	
	@Override
	public void setSettings(AsterixSettingsReader settings) {
		this.settings = settings;
	}

}
