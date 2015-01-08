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
package com.avanza.astrix.context;

import java.util.List;

import com.avanza.astrix.config.ConfigSource;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixConfigPlugin {
	
	/**
	 * Returns a list of config sources. All config sources defined
	 * here takes precedence over the standard config locations which is:
	 *  
	 * 1. Programatic settings on AstrixConfigurer
	 * 2. Classpath override mechanism
	 * 3. Astrix default values
	 * 
	 * The config sources will be queried in order returned.
	 * 
	 * @param uri
	 * @return
	 */
	List<ConfigSource> getConfigSources(String uri);
	
	
	// AstrixConfigPluginSettings
	
	// the.setting.name=avanza:avanza.jndi.primary=testapp01.test.aza.se:34000
	// the.setting.name=in-memory:213234
}
