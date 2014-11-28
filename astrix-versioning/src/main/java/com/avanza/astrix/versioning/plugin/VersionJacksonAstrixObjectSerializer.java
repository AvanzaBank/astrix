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

import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.provider.versioning.AstrixJsonApiMigration;
import com.avanza.astrix.provider.versioning.AstrixObjectMapperConfigurer;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
import com.avanza.astrix.provider.versioning.JacksonObjectMapperBuilder;
import com.avanza.astrix.versioning.JsonObjectMapper;
import com.avanza.astrix.versioning.VersionedJsonObjectMapper.VersionedObjectMapperBuilder;

public class VersionJacksonAstrixObjectSerializer implements AstrixObjectSerializer {

	private JsonObjectMapper objectMapper;
	private int version;

	public VersionJacksonAstrixObjectSerializer(AstrixVersioned versioningInfo) {
		Class<? extends AstrixJsonApiMigration>[] apiMigrationFactories = versioningInfo.apiMigrations();
		Class<? extends AstrixObjectMapperConfigurer> objectMapperConfigurerFactory = versioningInfo.objectMapperConfigurer();
		Class<? extends AstrixObjectSerializerConfigurer> serializerBuilder = versioningInfo.objectSerializerConfigurer();
		this.version = versioningInfo.version();
		try {
			if (serializerBuilder.equals(AstrixObjectSerializerConfigurer.class)) {
				this.objectMapper = buildObjectMapper(new LegacyClientAdapter(apiMigrationFactories, objectMapperConfigurerFactory));
			} else {
				this.objectMapper = buildObjectMapper(Jackson1ObjectSerializerConfigurer.class.cast(serializerBuilder.newInstance()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to init JsonObjectMapper", e);
		}
	}
	
	static class LegacyClientAdapter implements Jackson1ObjectSerializerConfigurer {
		
		private List<AstrixJsonApiMigration> apiMigrations = new ArrayList<>();
		private AstrixObjectMapperConfigurer astrixObjectMapperConfigurer;
		
		public LegacyClientAdapter(Class<? extends AstrixJsonApiMigration>[] apiMigrationFactories,
								   Class<? extends AstrixObjectMapperConfigurer> objectMapperConfigurerFactory) throws Exception {
			astrixObjectMapperConfigurer = objectMapperConfigurerFactory.newInstance();
			for (Class<? extends AstrixJsonApiMigration> apiMigrationFactory : apiMigrationFactories) {
				apiMigrations.add(apiMigrationFactory.newInstance());
			}
		}
		
		@Override
		public List<? extends AstrixJsonApiMigration> apiMigrations() {
			return apiMigrations;
		}
		@Override
		public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
			astrixObjectMapperConfigurer.configure(objectMapperBuilder);
		}
	}

	private JsonObjectMapper buildObjectMapper(Jackson1ObjectSerializerConfigurer serializerBuilder) {
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(serializerBuilder.apiMigrations());
		serializerBuilder.configure(objectMapperBuilder);
		return JsonObjectMapper.create(objectMapperBuilder.build());
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
