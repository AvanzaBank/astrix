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

import java.util.Objects;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class StringSetting implements Setting<String> {
	
	private final String name;
	private final String defaultValue;
	
	public StringSetting(String name, String defaultValue) {
		this.name = Objects.requireNonNull(name);
		this.defaultValue = defaultValue;
	}

	public static StringSetting create(String name, String defaultValue) {
		return new StringSetting(name, defaultValue);
	}
	
	@Override
	public DynamicStringProperty getFrom(DynamicConfig config) {
		return config.getStringProperty(name, defaultValue);
	}

	@Override
	public String name() {
		return name;
	}
	
	public String defaultValue() {
		return defaultValue;
	}

}
