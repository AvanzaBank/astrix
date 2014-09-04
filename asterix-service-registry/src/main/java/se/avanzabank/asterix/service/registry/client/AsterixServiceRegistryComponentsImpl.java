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
package se.avanzabank.asterix.service.registry.client;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;

@MetaInfServices(AsterixServiceRegistryComponents.class)
public class AsterixServiceRegistryComponentsImpl implements AsterixServiceRegistryComponents, AsterixPluginsAware {

	private AsterixPlugins plugins;

	@Override
	public AsterixServiceRegistryComponent getComponent(String name) {
		for (AsterixServiceRegistryComponent component : plugins.getPlugins(AsterixServiceRegistryComponent.class)) {
			if (component.getName().equals(name)) {
				return component;
			}
		}
		throw new IllegalStateException("No component found with name: " + name);
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

}
