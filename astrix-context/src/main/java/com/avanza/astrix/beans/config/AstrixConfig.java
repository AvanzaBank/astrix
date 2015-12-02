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
package com.avanza.astrix.beans.config;

import java.util.Map;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.config.BooleanSetting;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.config.DynamicStringProperty;
import com.avanza.astrix.config.IntSetting;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.StringSetting;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixConfig {

	DynamicLongProperty get(LongSetting setting);
	DynamicIntProperty get(IntSetting setting);
	DynamicBooleanProperty get(BooleanSetting setting);
	DynamicStringProperty get(StringSetting setting);
	
	DynamicStringProperty getStringProperty(String name, String defaultValue);
	
	/**
	 * Returns the DynamicConfig instance used by the current AstrixContext. <p>
	 * @return
	 */
	DynamicConfig getConfig();
	
	/**
	 * Allows providing programmatic settings after the AstrixContext is created,
	 * i.e the same type of settings that can be set on a AstrixConfigurer. Note
	 * that this settings might be overridden by other ConfigSources in the DynamicConfig
	 * instance used by the current AstrixContext.
	 * 
	 * @param setting
	 * @param value
	 */
	void set(String setting, String value);
	
	BeanConfiguration getBeanConfiguration(AstrixBeanKey<?> beanKey);

	void setDefaultBeanConfig(AstrixBeanKey<?> beanKey, Map<BeanSetting<?>, Object> defaultBeanSettingsOverride);
}	
