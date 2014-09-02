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
package se.avanzabank.service.suite.integration.tests.domain.apiruntime;

import org.codehaus.jackson.node.ObjectNode;

import se.avanzabank.service.suite.integration.tests.domain.api.LunchRestaurant;
import se.avanzabank.service.suite.provider.versioning.AstrixJsonApiMigration;
import se.avanzabank.service.suite.provider.versioning.AstrixJsonMessageMigration;

public class LunchApiV1Migration implements AstrixJsonApiMigration {

	@Override
	public int fromVersion() {
		return 1;
	}
	
	@Override
	public AstrixJsonMessageMigration<?>[] getMigrations() {
		return new AstrixJsonMessageMigration[] {
			new LunchRestaurantV1Migration()
		};
	}
	
	private static class LunchRestaurantV1Migration implements AstrixJsonMessageMigration<LunchRestaurant> {

		@Override
		public void upgrade(ObjectNode json) {
			json.put("foodType", "unknown");
		}
		
		@Override
		public void downgrade(ObjectNode json) {
			json.remove("foodType");
		}

		@Override
		public Class<LunchRestaurant> getJavaType() {
			return LunchRestaurant.class;
		}
	}
}


