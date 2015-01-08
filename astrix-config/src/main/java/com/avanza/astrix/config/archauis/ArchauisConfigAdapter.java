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

import com.avanza.astrix.config.DynamicConfigSource;
import com.avanza.astrix.config.DynamicStringPropertyListener;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ArchauisConfigAdapter implements DynamicConfigSource {
	
	private static final String LOOKUP_MISS = "_______LOOKUP_MISS______";
	private DynamicPropertyFactory dynamicPropertyFactory;
	
	public ArchauisConfigAdapter(DynamicPropertyFactory dynamicPropertyFactory) {
		this.dynamicPropertyFactory = dynamicPropertyFactory;
	}
	
	@Override
	public String get(final String name, final DynamicStringPropertyListener propertyChangeListener) {
		DynamicStringProperty stringProperty = dynamicPropertyFactory.getStringProperty(name, LOOKUP_MISS, new Runnable() {
			@Override
			public void run() {
				String newValue = dynamicPropertyFactory.getStringProperty(name, LOOKUP_MISS).get();
				if (LOOKUP_MISS.equals(newValue)) {
					propertyChangeListener.propertyChanged(null);
				} else {
					propertyChangeListener.propertyChanged(newValue);
				}
			}
		});
		String result = stringProperty.get();
		if (LOOKUP_MISS.equals(result)) {
			return null; 
		}
		return result;
	}

}
