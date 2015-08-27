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
package com.avanza.astrix.gs.remoting;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.context.AstrixStrategiesConfig;
import com.avanza.astrix.context.core.AsyncTypeConverter;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;
@MetaInfServices(AstrixContextPlugin.class)
public class GsRemotingModule implements AstrixContextPlugin {

	@Override
	public void registerStrategies(AstrixStrategiesConfig astrixContextConfig) {
	}

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, GsRemotingComponent.class);
		
		moduleContext.importType(ObjectSerializerFactory.class);
		moduleContext.importType(AstrixSpringContext.class);
		moduleContext.importType(BeanFaultToleranceFactory.class);
		moduleContext.importType(ClusteredProxyCache.class);
		moduleContext.importType(AstrixServiceActivator.class);
		moduleContext.importType(AsyncTypeConverter.class);
		
		moduleContext.export(ServiceComponent.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
