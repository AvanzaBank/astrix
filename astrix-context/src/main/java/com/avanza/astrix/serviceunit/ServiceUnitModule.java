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

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.context.AstrixStrategiesConfig;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.modules.ModuleContext;

@MetaInfServices(AstrixContextPlugin.class)
public class ServiceUnitModule implements AstrixContextPlugin {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceExporter.class, ServiceExporterImpl.class);
		moduleContext.bind(ServiceAdministrator.class, ServiceAdministratorImpl.class);
		moduleContext.bind(ServiceProviderPlugin.class, GenericServiceProviderPlugin.class); // TODO: Should ServiceProviderPlugin be a plugin?
		
		moduleContext.importType(ServiceComponentRegistry.class);
		moduleContext.importType(AstrixConfig.class);
		
		moduleContext.export(ServiceAdministrator.class);
		moduleContext.export(ServiceExporter.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

	@Override
	public void registerStrategies(AstrixStrategiesConfig astrixContextConfig) {
	}

}
