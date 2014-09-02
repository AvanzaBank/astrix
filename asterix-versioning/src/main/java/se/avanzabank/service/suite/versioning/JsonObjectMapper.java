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
package se.avanzabank.service.suite.versioning;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class JsonObjectMapper {
	
	private final Impl impl;
	
	private JsonObjectMapper(Impl impl) {
		this.impl = impl;
	}

	public String serialize(Object object, int toVersion) {
		try {
			return impl.serialize(object, toVersion);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize: " + object + ".", e);
		}
	}
	
	public <T> T deserialize(String json, Class<T> target, int fromVersion) {
		try {
			return impl.deserialize(json, target, fromVersion);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize: " + json + " into type: " + target, e);
		}
	}
	
	public static JsonObjectMapper create(Impl impl) {
		return new JsonObjectMapper(impl);
	}
	
	public static JsonObjectMapper simpleJacksonObjectMapper(ObjectMapper objectMapper) {
		return new JsonObjectMapper(new SimpleJacksonJsonMessageMapper(objectMapper));
	}
	
	// TODO: remove direct version: replaced by AstrixObjectSerializer abstraction
	public static JsonObjectMapper direct() {
		return direct(new DirectMessageMapper());
	}
	
	public static JsonObjectMapper direct(DirectMessageMapper messageMapper) {
		return new JsonObjectMapper(messageMapper);
	}
	
	public interface Impl {
		String serialize(Object object, int toVersion) throws Exception;
		<T> T deserialize(String json, Class<T> target, int fromVersion) throws Exception;
	}
	
	private static class SimpleJacksonJsonMessageMapper implements Impl {
		
		private ObjectMapper objectMapper;

		public SimpleJacksonJsonMessageMapper(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public String serialize(Object object, int toVersion) throws Exception {
			return objectMapper.writeValueAsString(object);
		}

		@Override
		public <T> T deserialize(String json, Class<T> target, int fromVersion) throws Exception {
			return objectMapper.readValue(json, target);
		}
	}
	
	public static class DirectMessageMapper implements Impl {
		
		// TODO: rename to IdentityMessageMapper?
		
		private Map<String, Object> mappings = new HashMap<>();
		private AtomicInteger idSequence = new AtomicInteger();

		@Override
		public String serialize(Object object, int toVersion) throws Exception {
			String messageId = Integer.toString(idSequence.incrementAndGet());
			this.mappings.put(messageId, object);
			return messageId;
		}

		@Override
		public <T> T deserialize(String json, Class<T> target, int fromVersion) throws Exception {
			T mapping = target.cast(this.mappings.get(json));
			if (mapping == null) {
				throw new IllegalArgumentException("No mapping found for json: " + json + ". An object must be written to the same DirectMessageMapper instance before deserializeing");
			}
			return mapping;
		}		
	}
	
	
}
