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
package se.avanzabank.service.suite.remoting.server;

import java.util.List;

import se.avanzabank.service.suite.provider.versioning.AstrixJsonApiMigration;
import se.avanzabank.service.suite.provider.versioning.AstrixObjectMapperConfigurer;

public class AstrixRemoteServiceProvider {
	
	private List<AstrixJsonApiMigration> apiMigrations;
	private AstrixObjectMapperConfigurer objectMapperConfigurer;
	
	public AstrixRemoteServiceProvider(List<AstrixJsonApiMigration> apiMigrations, AstrixObjectMapperConfigurer objectMapperConfigurer) {
		this.apiMigrations = apiMigrations;
		this.objectMapperConfigurer = objectMapperConfigurer;
	}
	
	public List<AstrixJsonApiMigration> getApiMigrations() {
		return apiMigrations;
	}

	public AstrixObjectMapperConfigurer getObjectMapperConfigurer() {
		return objectMapperConfigurer;
	}

}
