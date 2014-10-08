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
package se.avanzabank.asterix.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kohsuke.MetaInfServices;

@MetaInfServices(AsterixServiceTransports.class)
public class AsterixServiceTransportsImpl implements AsterixServiceTransports, AsterixPluginsAware {

	private final Map<String, AsterixServiceTransport> transportByName = new ConcurrentHashMap<>();
	
	@Override
	public AsterixServiceTransport getTransport(String name) {
		AsterixServiceTransport serviceTransport = transportByName.get(name);
		if (serviceTransport == null) {
			throw new IllegalStateException("No transport found with name: " + name);
		}
		return serviceTransport;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		for (AsterixServiceTransport serviceTransport : plugins.getPlugins(AsterixServiceTransport.class)) {
			transportByName.put(serviceTransport.getName(), serviceTransport);
		}
	}

}
