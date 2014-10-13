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
package se.avanzabank.asterix.gs;

import org.openspaces.core.GigaSpace;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;

public class GsServiceExporter implements AsterixServiceExporterBean {

	@Override
	public void register(Object provider, AsterixApiDescriptor apiDescriptor, Class<?> providedApi) {
		if (!providedApi.equals(GigaSpace.class)) {
			throw new IllegalArgumentException("Cannot export: " + providedApi);
		}
		// Nothing required to export GigaSpace.
	}
	
	@Override
	public String getComponent() {
		return AsterixServiceComponentNames.GS;
	}

}
