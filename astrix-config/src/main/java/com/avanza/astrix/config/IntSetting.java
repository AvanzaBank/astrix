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

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class IntSetting implements Setting<Integer> {
	
	private final String name;
	private final int defaultValue;
	
	private IntSetting(String name, int defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public static IntSetting create(String name, int defaultValue) {
		return new IntSetting(name, defaultValue);
	}

	@Override
	public DynamicIntProperty getFrom(DynamicConfig config) {
		return config.getIntProperty(name, defaultValue);
	}

	@Override
	public String name() {
		return name;
	}
	
	public int defaultValue() {
		return defaultValue;
	}


}
