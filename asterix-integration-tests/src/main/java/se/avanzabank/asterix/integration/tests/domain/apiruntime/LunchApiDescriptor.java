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
package se.avanzabank.asterix.integration.tests.domain.apiruntime;

import se.avanzabank.asterix.integration.tests.domain.api.LunchService;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;
import se.avanzabank.asterix.provider.versioning.AsterixVersioned;


// API:t är versionshanterat
@AsterixVersioned(
	apiMigrations = {
		LunchApiV1Migration.class
	},	
	version = 2,
	objectMapperConfigurer = LunchApiObjectMapperConfigurer.class
)
// Tjänsten publiceras och kan slås upp via tjänsteregistret
@AsterixServiceRegistryApi(
	exportedApis = {
		LunchService.class
	}	
)
// TODO: kan vi få bort behovet av att annotera denna med @AsterixGsApiDescriptor?
// Som det ser ut nu används denna på server-sidan för att konfa astrix
@AsterixRemoteApiDescriptor
public class LunchApiDescriptor {
	
	/*
	 * TODO: how to export pub-sub/services? local-snapshots? Other not yet known aspects? 
	 * We need an extension point for new types of 'services'. Annotate each exported api
	 * with info about what type of api it is?
	 */
	
}


