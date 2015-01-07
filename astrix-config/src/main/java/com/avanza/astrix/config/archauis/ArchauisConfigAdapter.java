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
package com.avanza.astrix.config.archauis;

import com.avanza.astrix.config.ConfigSource;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ArchauisConfigAdapter implements ConfigSource {
	
	private DynamicPropertyFactory dynamicPropertyFactory;
	
	public ArchauisConfigAdapter(DynamicPropertyFactory dynamicPropertyFactory) {
		this.dynamicPropertyFactory = dynamicPropertyFactory;
	}
	
	@Override
	public String get(final String name, final String defaultValue, final DynamicPropertyListener propertyChangeListener) {
		DynamicStringProperty stringProperty = dynamicPropertyFactory.getStringProperty(name, defaultValue, new Runnable() {
			@Override
			public void run() {
				String newValue = dynamicPropertyFactory.getStringProperty(name, defaultValue).get();
				propertyChangeListener.propertyChanged(newValue);
			}
		});
		return stringProperty.get();
	}

}
