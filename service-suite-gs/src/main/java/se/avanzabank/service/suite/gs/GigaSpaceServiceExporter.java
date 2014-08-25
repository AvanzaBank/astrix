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
package se.avanzabank.service.suite.gs;

import java.util.Arrays;
import java.util.List;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.service.suite.bus.client.AstrixServiceProperties;
import se.avanzabank.service.suite.provider.context.ServiceExporter;

public class GigaSpaceServiceExporter implements ServiceExporter {
	
	private final GigaSpace space;

	@Autowired
	public GigaSpaceServiceExporter(GigaSpace space) {
		this.space = space;
	}

	@Override
	public List<AstrixServiceProperties> getProvidedServices() {
		return Arrays.asList(GsBinder.createProperties(space));
	}

}
