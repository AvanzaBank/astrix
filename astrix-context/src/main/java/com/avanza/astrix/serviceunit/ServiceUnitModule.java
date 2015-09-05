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
package com.avanza.astrix.serviceunit;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;

public class ServiceUnitModule implements Module {

	private final AstrixApplicationDescriptor applicationDescriptor;
	
	public ServiceUnitModule(AstrixApplicationDescriptor applicationDescriptor) {
		this.applicationDescriptor = applicationDescriptor;
	}

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceExporter.class, ServiceExporterImpl.class);
		moduleContext.bind(ServiceAdministrator.class, ServiceAdministratorImpl.class);
		moduleContext.bind(AstrixApplicationDescriptor.class, applicationDescriptor);
		moduleContext.bind(ServiceProviderPlugin.class, GenericServiceProviderPlugin.class); // TODO: Should ServiceProviderPlugin be a plugin?
		
		moduleContext.importType(ServiceComponentRegistry.class);
		moduleContext.importType(AstrixConfig.class);
		
		moduleContext.export(ServiceAdministrator.class);
		moduleContext.export(ServiceExporter.class);
	}

}
