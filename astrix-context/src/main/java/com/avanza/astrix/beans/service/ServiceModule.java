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
package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfigurations;
import com.avanza.astrix.context.core.AstrixMBeanExporter;
import com.avanza.astrix.context.core.ReactiveTypeConverter;
import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;

public class ServiceModule implements Module {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponentRegistry.class, ServiceComponents.class);
		moduleContext.bind(ServiceDiscoveryMetaFactory.class, ServiceDiscoveryMetaFactoryImpl.class);
		moduleContext.bind(ServiceMetaFactory.class, ServiceMetaFactoryImpl.class);

		// Extension points		
		moduleContext.importType(ServiceComponent.class);
		moduleContext.importType(ServiceDiscoveryMetaFactoryPlugin.class);  	  
		moduleContext.importType(ServiceBeanProxyFactory.class);
		
		// Dependencies
		moduleContext.importType(ObjectSerializerFactory.class);
		moduleContext.importType(AstrixConfig.class); 			  
		moduleContext.importType(AstrixMBeanExporter.class);  	  
		moduleContext.importType(ReactiveTypeConverter.class);  	  
		moduleContext.importType(BeanConfigurations.class);
		
		moduleContext.export(ServiceDiscoveryMetaFactory.class);
		moduleContext.export(ServiceMetaFactory.class);
		moduleContext.export(ServiceComponentRegistry.class);
	}

}
