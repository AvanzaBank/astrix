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
package com.avanza.astrix.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * This is a jvm global registry for ConfigSources, mainly intended to support testing.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class GlobalConfigSourceRegistry {
	
	private final static AtomicLong idGen = new AtomicLong();
	private final static Map<String, ConfigSource> configSourceById = new ConcurrentHashMap<>();
	
	private GlobalConfigSourceRegistry() {
	}
	
	public static ConfigSource getConfigSource(String configSourceId) {
		return configSourceById.get(configSourceId);
	}

	public static String register(ConfigSource configSource) {
		String id = Long.toString(idGen.incrementAndGet());
		configSourceById.put(id, configSource);
		return id;
	}

}
