/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.registry;

import org.codehaus.jackson.node.ObjectNode;

import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.versioning.jackson1.AstrixJsonApiMigration;
import com.avanza.astrix.versioning.jackson1.AstrixJsonMessageMigration;

public class ServiceRegistryV1ApiMigration implements AstrixJsonApiMigration {

	@Override
	public int fromVersion() {
		return 1;
	}

	@Override
	public AstrixJsonMessageMigration<?>[] getMigrations() {
		return new AstrixJsonMessageMigration[] {
			new AstrixServiceRegistryEntryV1Migration()		
		};
	}
	
	public static class AstrixServiceRegistryEntryV1Migration implements AstrixJsonMessageMigration<AstrixServiceRegistryEntry> {
		@Override
		public Class<AstrixServiceRegistryEntry> getJavaType() {
			return AstrixServiceRegistryEntry.class;
		}

		@Override
		public void upgrade(ObjectNode json) {
			// ApplicationInstanceId concept was introduced to uniquely identify a service.
			// Old clients will not set the property, but it was expected that a service (api + qualifier)
			// was only provided by a single application instance, hence we use it as id.
			String qualifier = json.get("serviceProperties").get("_qualifier").getTextValue();
			String api = json.get("serviceProperties").get("_api").getTextValue();
			String applicationInstanceId = api + "_" + qualifier; 
			ObjectNode.class.cast(json.get("serviceProperties")).put(ServiceProperties.APPLICATION_INSTANCE_ID, applicationInstanceId);
			
			// ServiceState was introduced to allow multiple servers providing same service. 
			// We assume old servers are not run concurrently and hence alwasy assume them to be active
//			ObjectNode.class.cast(json.get("serviceProperties")).put(ServiceProperties., ServiceState.ACTIVE);
		}

		@Override
		public void downgrade(ObjectNode json) {
			ObjectNode.class.cast(json.get("serviceProperties")).remove(ServiceProperties.APPLICATION_INSTANCE_ID);
//			ObjectNode.class.cast(json.get("serviceProperties")).remove(ServiceProperties.SERVICE_STATE);
		}
	}

}
