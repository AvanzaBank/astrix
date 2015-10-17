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
package com.avanza.astrix.gs;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.context.AstrixStrategiesConfig;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.spring.AstrixSpringContext;
/**
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(AstrixContextPlugin.class)
public class GsModule implements AstrixContextPlugin {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, GsComponent.class);
		moduleContext.bind(ClusteredProxyBinder.class, GsComponent.class);
		moduleContext.bind(ClusteredProxyCache.class, ClusteredProxyCacheImpl.class);
		moduleContext.bind(ReactiveTypeHandlerPlugin.class, AsyncFutureTypeHandler.class);
		
		moduleContext.importType(AstrixSpringContext.class);
		moduleContext.importType(BeanFaultToleranceFactory.class);
		
		moduleContext.export(ServiceComponent.class);
		moduleContext.export(ClusteredProxyBinder.class);
		moduleContext.export(ClusteredProxyCache.class);
		moduleContext.export(ReactiveTypeHandlerPlugin.class);
	}

	@Override
	public void registerStrategies(AstrixStrategiesConfig astrixContextConfig) {
	}

}
