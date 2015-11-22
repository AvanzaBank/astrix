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
package com.avanza.astrix.versioning.jackson2;

import com.avanza.astrix.versioning.jackson2.JsonMessageMigrator.Builder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VersionedJsonObjectMapper implements JsonObjectMapper.Impl {
	
	private ObjectMapper migratingMapper;
	private ThreadLocal<Integer> versionHolder;
	
	public VersionedJsonObjectMapper(ThreadLocal<Integer> versionHolder,
								 ObjectMapper migratingMapper) {
				this.versionHolder = versionHolder;
				this.migratingMapper = migratingMapper;
	}

	@Override
	public String serialize(Object object, int toVersion) throws Exception {
		versionHolder.set(toVersion);
		try {
			return migratingMapper.writeValueAsString(object);
		} finally {
			versionHolder.remove();
		}
	}

	@Override
	public <T> T deserialize(String json, Type target, int fromVersion) throws Exception {
		versionHolder.set(fromVersion);
		try {
			JavaType javaType = migratingMapper.getTypeFactory().constructType(target);
			return migratingMapper.readValue(json, javaType);
		} finally {
			versionHolder.remove();
		}
	}
	
	// TODO: document whats going on in this class (the migrating object mapper)
	
	static class JsonSerializerHolder<T> {
		
		private Class<T> type;
		private JsonSerializer<T> serializer;

		public JsonSerializerHolder(Class<T> type, JsonSerializer<T> serializer) {
			this.type = type;
			this.serializer = serializer;
		}

		public void register(SimpleModule module) {
			module.addSerializer(type, serializer);
		}
		
	}
	
	
	static class JsonDeserializerHolder<T> {
		
		private Class<T> type;
		private JsonDeserializer<T> deserializer;

		public JsonDeserializerHolder(Class<T> type, JsonDeserializer<T> deserializer) {
			this.type = type;
			this.deserializer = deserializer;
		}

		public void register(SimpleModule module) {
			module.addDeserializer(type, deserializer);
		}
	}

	static class MigratingJsonSerializer<T> extends JsonSerializer<T> {
		
		private ObjectMapper rawMapper;
		private JsonMessageMigrator<T> migrator;
		private ThreadLocal<Integer> versionHolder;
		
		public MigratingJsonSerializer(ObjectMapper rawMapper,
				JsonMessageMigrator<T> migrator,
				ThreadLocal<Integer> versionHolder) {
			this.rawMapper = rawMapper;
			this.migrator = migrator;
			this.versionHolder = versionHolder;
		}

		public static <T> MigratingJsonSerializer<T> create(ObjectMapper rawMapper, JsonMessageMigrator<T> migrator, ThreadLocal<Integer> versionHolder) {
			return new MigratingJsonSerializer<>(rawMapper, migrator, versionHolder);
		}
		
		@Override
		public void serialize(T value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			ObjectNode objectNode = rawMapper.convertValue(value, ObjectNode.class);
			migrator.downgrade(objectNode, getVersion());
			jgen.writeObject(objectNode);					
		}



		int getVersion() {
			return versionHolder.get();
		}
	}
	
	static class MigratingJsonDeserializer<T> extends JsonDeserializer<T> {
		
		private ObjectMapper rawMapper;
		private JsonMessageMigrator<T> migrator;
		private ThreadLocal<Integer> versionHolder;
		
		public MigratingJsonDeserializer(ObjectMapper rawMapper,
				JsonMessageMigrator<T> migrator,
				ThreadLocal<Integer> versionHolder) {
			this.rawMapper = rawMapper;
			this.migrator = migrator;
			this.versionHolder = versionHolder;
		}

		public static <T> MigratingJsonDeserializer<T> create(ObjectMapper rawMapper, JsonMessageMigrator<T> migrator, ThreadLocal<Integer> versionHolder) {
			return new MigratingJsonDeserializer<>(rawMapper, migrator, versionHolder);
		}
		
		@Override
		public T deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectNode objectNode = jp.readValueAs(ObjectNode.class);
			migrator.upgrade(objectNode, getVersion());
			return rawMapper.convertValue(objectNode, migrator.getJavaType());					
		}
		
		int getVersion() {
			return versionHolder.get();
		}
	}
	
	static class MessageMigratorsBuilder {
		
		Map<Class<?>, JsonMessageMigrator.Builder<?>> buildersByType = new HashMap<Class<?>, JsonMessageMigrator.Builder<?>>();
		
		MessageMigratorsBuilder registerAll(List<? extends AstrixJsonApiMigration> migrations) {
			for (AstrixJsonApiMigration apiMigration : migrations) {
				int version = apiMigration.fromVersion();
				for (AstrixJsonMessageMigration<?> messageMigration : apiMigration.getMigrations()) {
					register(version, messageMigration);
				}
			}
			return this;
		}

		<T> void register(int version, AstrixJsonMessageMigration<T> messageMigration) {
			Builder<T> builder = (Builder<T>) buildersByType.get(messageMigration.getJavaType());
			if (builder == null) {
				builder = new Builder<>(messageMigration.getJavaType());
				buildersByType.put(messageMigration.getJavaType(), builder);
			}
			builder.addMigration(messageMigration, version);
		}
		
		ConcurrentMap<Class<?>, JsonMessageMigrator<?>> build() {
			ConcurrentMap<Class<?>, JsonMessageMigrator<?>> migratorsByType = new ConcurrentHashMap<Class<?>, JsonMessageMigrator<?>>();
			for (JsonMessageMigrator.Builder<?> builder : this.buildersByType.values()) {
				JsonMessageMigrator<?> jsonMessageMigrator = builder.build();
				migratorsByType.put(jsonMessageMigrator.getJavaType(), jsonMessageMigrator);
			}
			return migratorsByType;
		}
	}
	
	public static class VersionedObjectMapperBuilder implements JacksonObjectMapperBuilder {
		
		private List<JsonSerializerHolder<?>> serializers = new ArrayList<>();
		private List<JsonDeserializerHolder<?>> deserializers = new ArrayList<>();
		private ConcurrentMap<Class<?>, JsonMessageMigrator<?>> migratorsByType;
		
		public VersionedObjectMapperBuilder(List<? extends AstrixJsonApiMigration> migrations) {
			this.migratorsByType = new MessageMigratorsBuilder().registerAll(migrations).build();
		}

		@Override
		public <T> void addSerializer(Class<T> type, JsonSerializer<T> serializer) {
			this.serializers.add(new JsonSerializerHolder<>(type, serializer));
		}

		@Override
		public <T> void addDeserializer(Class<T> type, JsonDeserializer<T> deserializer) {
			this.deserializers.add(new JsonDeserializerHolder<>(type, deserializer));
		}
		
		public VersionedJsonObjectMapper build() {
			ThreadLocal<Integer> versionHolder = new ThreadLocal<>();
			ObjectMapper rawMapper = buildRaw();
			ObjectMapper migratingMapper = buildMigratingMapper(rawMapper, versionHolder);
			return new VersionedJsonObjectMapper(versionHolder, migratingMapper);
		}
		
		private ObjectMapper buildMigratingMapper(ObjectMapper rawMapper, ThreadLocal<Integer> versionHolder) {
			SimpleModule module = new SimpleModule("Astrix-migratingModule", new Version(1,0,0, ""));
			for (JsonMessageMigrator<?> migrator : this.migratorsByType.values()) {
				registerSerializerAndDeserializer(rawMapper, versionHolder, module, migrator);
			}
			// register custom serializers/deserializers for all custom types without migrator since those won't be intercepted by migratingObjectMapper
			for (JsonDeserializerHolder<?> deserializer : this.deserializers) {
				if (!this.migratorsByType.containsKey(deserializer.type)) {
					deserializer.register(module);
				}
			}
			for (JsonSerializerHolder<?> serializer : this.serializers) {
				if (!this.migratorsByType.containsKey(serializer.type)) {
					serializer.register(module);
				}
			}

			ObjectMapper result = new ObjectMapper();
			result.registerModule(module);
			return result;
		}

		private <T> void registerSerializerAndDeserializer(ObjectMapper rawMapper,
														   ThreadLocal<Integer> versionHolder, 
														   SimpleModule module,
														   JsonMessageMigrator<T> migrator) {
			module.addSerializer(migrator.getJavaType(), MigratingJsonSerializer.create(rawMapper, migrator, versionHolder));
			module.addDeserializer(migrator.getJavaType(), MigratingJsonDeserializer.create(rawMapper, migrator, versionHolder));
		}

		private ObjectMapper buildRaw() {
			SimpleModule rawModule = new SimpleModule("Astrix-rawModule", new Version(1,0,0, ""));
			for (JsonDeserializerHolder<?> deserializer : this.deserializers) {
				deserializer.register(rawModule);
			}
			for (JsonSerializerHolder<?> serializer : this.serializers) {
				serializer.register(rawModule);
			}
			ObjectMapper rawMapper = new ObjectMapper();
			rawMapper.registerModule(rawModule);
			return rawMapper;
		}
		
	}

//	public static JsonObjectMapper create(AstrixRemotingServerApi jsonObjectMapperFactory) {
//		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(jsonObjectMapperFactory.getMigrations());
//		jsonObjectMapperFactory.configure(objectMapperBuilder);
//		return JsonObjectMapper.create(objectMapperBuilder.build());
//	}


}
