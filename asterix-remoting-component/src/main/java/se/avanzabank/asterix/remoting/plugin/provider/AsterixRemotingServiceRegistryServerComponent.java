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
package se.avanzabank.asterix.remoting.plugin.provider;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.gs.GigaSpaceServiceRegistryExporter;
import se.avanzabank.asterix.provider.context.AsterixServiceRegistryServerComponent;
import se.avanzabank.asterix.provider.context.ServiceRegistryExporter;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;

@MetaInfServices(AsterixServiceRegistryServerComponent.class)
public class AsterixRemotingServiceRegistryServerComponent implements AsterixServiceRegistryServerComponent {

	@Override
	public List<Class<? extends ServiceRegistryExporter>> getRequiredExporterClasses() {
		return Arrays.<Class<? extends ServiceRegistryExporter>>asList(
				AsterixRemotingServiceRegistryExporter.class,
				GigaSpaceServiceRegistryExporter.class);
	}

	@Override
	public boolean isActivatedBy(AsterixApiDescriptor descriptor) {
		return descriptor.isAnnotationPresent(AsterixRemoteApiDescriptor.class);
	}

}
