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
package com.avanza.astrix.versioning.jackson1;

import java.lang.reflect.Type;
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
	
	public <T> T deserialize(String json, Type target, int fromVersion) {
		try {
			return impl.deserialize(json, target, fromVersion);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize: " + json + " into type: " + target, e);
		}
	}
	
	public static JsonObjectMapper create(Impl impl) {
		return new JsonObjectMapper(impl);
	}
	
	
	public interface Impl {
		String serialize(Object object, int toVersion) throws Exception;
		<T> T deserialize(String json, Type target, int fromVersion) throws Exception;
	}
	
}
