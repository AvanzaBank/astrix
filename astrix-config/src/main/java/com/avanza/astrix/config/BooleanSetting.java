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
package com.avanza.astrix.config;

import java.util.Objects;

public class BooleanSetting implements Setting<Boolean> {
	
	private final String name;
	private final boolean defaultValue;
	
	private BooleanSetting(String name, boolean defaultValue) {
		this.name = Objects.requireNonNull(name);
		this.defaultValue = defaultValue;
	}
	
	public static BooleanSetting create(String name, boolean defaultValue) {
		return new BooleanSetting(name, defaultValue);
	}

	@Override
	public DynamicBooleanProperty getFrom(DynamicConfig config) {
		return config.getBooleanProperty(this.name, this.defaultValue);
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public Boolean defaultValue() {
		return Boolean.valueOf(defaultValue);
	}
	

}
