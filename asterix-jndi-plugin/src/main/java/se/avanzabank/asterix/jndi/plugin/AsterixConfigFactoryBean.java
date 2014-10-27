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
package se.avanzabank.asterix.jndi.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFactoryBeanPlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceComponents;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixSettingsReader;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AsterixConfigFactoryBean<T> implements AsterixFactoryBeanPlugin<T>, AsterixPluginsAware {
	
	private AsterixPlugins plugins;
	private String  entryName;
	private AsterixApiDescriptor descriptor;
	private Class<T> api;
	private AsterixSettingsReader settings;

	public AsterixConfigFactoryBean(String entryName, AsterixApiDescriptor descriptor, Class<T> beanType, AsterixSettingsReader settings) {
		this.entryName = entryName;
		this.descriptor = descriptor;
		this.api = beanType;
		this.settings = settings;
	}

	@Override
	public T create(String optionalQualifier) {
		AsterixServiceProperties serviceProperties = lookup();
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Missing entry in jndi: Entry-name=%s", entryName));
		}
		AsterixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return serviceComponent.createService(descriptor, api, serviceProperties);
	}

	private AsterixServiceComponent getServiceComponent(AsterixServiceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("AsterixServiceProperties retreived from jndi does not define a component: " + serviceProperties.getProperties());
		}
		return plugins.getPlugin(AsterixServiceComponents.class).getComponent(componentName);
	}
	

	private AsterixServiceProperties lookup() {
		Properties properties = lookup(entryName);
		if (properties == null) {
			throw new IllegalStateException("Config entry not defined: " + this.entryName + ". Config: " + this.settings);
		}
		return new AsterixServiceProperties(toMap(properties));
	}
	
	private Properties lookup(String name) {
		return this.settings.getProperties(name);
	}

	private Map<String, String> toMap(Properties properties) {
		Map<String, String> result = new HashMap<String, String>();
		for (String key : properties.stringPropertyNames()) {
			result.put(key, properties.getProperty(key));
		}
		return result;
	}

	@Override
	public Class<T> getBeanType() {
		return this.api;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

}
