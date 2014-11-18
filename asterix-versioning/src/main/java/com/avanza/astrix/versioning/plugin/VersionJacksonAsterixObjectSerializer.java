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
package com.avanza.astrix.versioning.plugin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.avanza.astrix.core.AsterixObjectSerializer;
import com.avanza.astrix.provider.versioning.AsterixJsonApiMigration;
import com.avanza.astrix.provider.versioning.AsterixObjectMapperConfigurer;
import com.avanza.astrix.provider.versioning.AsterixVersioned;
import com.avanza.astrix.versioning.JsonObjectMapper;
import com.avanza.astrix.versioning.VersionedJsonObjectMapper.VersionedObjectMapperBuilder;

public class VersionJacksonAsterixObjectSerializer implements AsterixObjectSerializer {

	private JsonObjectMapper objectMapper;
	private int version;

	public VersionJacksonAsterixObjectSerializer(AsterixVersioned versioningInfo) {
		Class<? extends AsterixJsonApiMigration>[] apiMigrationFactories = versioningInfo.apiMigrations();
		Class<? extends AsterixObjectMapperConfigurer> objectMapperConfigurerFactory = versioningInfo.objectMapperConfigurer();
		this.version = versioningInfo.version();
		this.objectMapper = buildObjectMapper(apiMigrationFactories, objectMapperConfigurerFactory);
	}

	private JsonObjectMapper buildObjectMapper(
			Class<? extends AsterixJsonApiMigration>[] apiMigrationFactories,
			Class<? extends AsterixObjectMapperConfigurer> objectMapperConfigurerFactory) {
		try {
			AsterixObjectMapperConfigurer asterixObjectMapperConfigurer = objectMapperConfigurerFactory.newInstance();
			List<AsterixJsonApiMigration> apiMigrations = new ArrayList<>();
			for (Class<? extends AsterixJsonApiMigration> apiMigrationFactory : apiMigrationFactories) {
				apiMigrations.add(apiMigrationFactory.newInstance());
			}
			VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
			asterixObjectMapperConfigurer.configure(objectMapperBuilder);
			return JsonObjectMapper.create(objectMapperBuilder.build());
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to init JsonObjectMapper", e);
		} finally {
		}
	}

	@Override
	public <T> T deserialize(Object element, Type type, int fromVersion) {
		if (fromVersion == NoVersioningSupport.NO_VERSIONING) {
			return (T) element;
		}
		return objectMapper.deserialize((String) element, type, fromVersion);
	}

	@Override
	public Object serialize(Object element, int version) {
		if (version == NoVersioningSupport.NO_VERSIONING) {
			return element;
		}
		return objectMapper.serialize(element, version);
	}

	@Override
	public int version() {
		return version;
	}

}
