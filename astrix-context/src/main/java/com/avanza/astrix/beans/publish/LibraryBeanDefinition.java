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
package com.avanza.astrix.beans.publish;

import java.util.Map;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.factory.StandardFactoryBean;

public class LibraryBeanDefinition<T> {
	
	private final StandardFactoryBean<T> factory;
	private final Map<BeanSetting<?>, Object> defaultBeanSettingsOverride;
	
	public LibraryBeanDefinition(StandardFactoryBean<T> factory, Map<BeanSetting<?>, Object> defaultBeanSettingsOverride)  {
		this.factory = factory;
		this.defaultBeanSettingsOverride = defaultBeanSettingsOverride;
	}

	public FactoryBean<T> getFactory() {
		return factory;
	}
	
	public Map<BeanSetting<?>, Object> getDefaultBeanSettingsOverride() {
		return defaultBeanSettingsOverride;
	}
	
	public AstrixBeanKey<T> getBeanKey() {
		return factory.getBeanKey();
	}


}
