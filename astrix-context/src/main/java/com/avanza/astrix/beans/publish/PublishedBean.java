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
import com.avanza.astrix.beans.factory.DynamicFactoryBean;
import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.factory.StandardFactoryBean;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class PublishedBean {

	private final FactoryBean<?> factory;
	private final Map<BeanSetting<?>, Object> defaultBeanSettingsOverride;
	
	public PublishedBean(FactoryBean<?> factory, Map<BeanSetting<?>, Object> defaultBeanSettingsOverride)  {
		this.factory = factory;
		this.defaultBeanSettingsOverride = defaultBeanSettingsOverride;
	}

	public FactoryBean<?> getFactory() {
		return factory;
	}
	
	public Map<BeanSetting<?>, Object> getDefaultBeanSettingsOverride() {
		return defaultBeanSettingsOverride;
	}

	public AstrixBeanKey<?> getBeanKey() {
		if (factory instanceof StandardFactoryBean) {
			return StandardFactoryBean.class.cast(factory).getBeanKey();
		}
		if (factory instanceof DynamicFactoryBean) {
			return AstrixBeanKey.create(DynamicFactoryBean.class.cast(factory).getType(), "*");
		}
		throw new RuntimeException("Unknown factory type: " + factory.getClass().getName());
	}
	
}
