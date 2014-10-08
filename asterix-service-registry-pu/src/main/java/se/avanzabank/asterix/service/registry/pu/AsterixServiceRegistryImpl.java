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
package se.avanzabank.asterix.service.registry.pu;

import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.provider.core.AsterixServiceExport;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteServiceExport;
import se.avanzabank.asterix.service.registry.app.ServiceKey;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistry;

//@AsterixRemoteServiceExport(AsterixServiceRegistry.class)
@AsterixServiceExport(AsterixServiceRegistry.class)
public class AsterixServiceRegistryImpl implements AsterixServiceRegistry {
	
	private final GigaSpace gigaSpace;
	
	@Autowired
	public AsterixServiceRegistryImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public <T> AsterixServiceProperties lookup(Class<T> type) {
		return lookup(type, null);
	}
	
	@Override
	public <T> AsterixServiceProperties lookup(@Routing Class<T> type, String qualifier) {
		ServiceProperitesInfo info = gigaSpace.readById(ServiceProperitesInfo.class, new ServiceKey(type.getName(), qualifier));
		if (info == null) {
			return null; // TODO: handle non registered services
		}
		return info.getProperties();
	}

	@Override
	public <T> void register(Class<T> api, AsterixServiceProperties properties) {
		ServiceProperitesInfo info = new ServiceProperitesInfo();
		info.setApiType(properties.getApi());
		// TODO: this method signature is weird. It used apiType from fist 
		// argument and qualifier from properties. It does not make sense.
		info.setServiceKey(new ServiceKey(api.getName(), properties.getQualifier()));
		info.setProperties(properties);
		// TODO: lease
		// TODO: qualifier
		gigaSpace.write(info);
	}

}
