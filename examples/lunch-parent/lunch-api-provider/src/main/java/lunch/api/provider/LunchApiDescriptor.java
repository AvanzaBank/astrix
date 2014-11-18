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
package lunch.api.provider;

import lunch.api.LunchService;

import com.avanza.astrix.provider.core.AstrixServiceRegistryApi;
import com.avanza.astrix.provider.versioning.AstrixVersioned;


// The API is versioned.
@AstrixVersioned(
	apiMigrations = {
		LunchApiV1Migration.class
	},	
	version = 2,
	objectMapperConfigurer = LunchApiObjectMapperConfigurer.class
)
// The service is exported to the service-registry. Consumers queries the service-registry to bind to servers.
@AstrixServiceRegistryApi(
	exportedApis = {
		LunchService.class
	}
)
public class LunchApiDescriptor {
}


