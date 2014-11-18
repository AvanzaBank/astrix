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
package com.avanza.astrix.remoting.component.provider;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixServicePropertiesBuilder;
import com.avanza.astrix.gs.GsBinder;

public class AstrixRemotingServiceRegistryExporter implements AstrixServicePropertiesBuilder {
	
	public static final String SPACE_NAME_PROPERTY = "space";
	
	private GigaSpace gigaSpace;
	
	@Autowired
	public AstrixRemotingServiceRegistryExporter(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public boolean supportsAsyncApis() {
		return true;
	}

	@Override
	public AstrixServiceProperties buildServiceProperties(Class<?> providedApi) {
		AstrixServiceProperties serviceProperties = GsBinder.createProperties(this.gigaSpace);
		serviceProperties.setQualifier(null);
		return serviceProperties;
	}
	
}
