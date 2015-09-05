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
package com.avanza.astrix.spring;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.context.AstrixStrategiesConfig;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.serviceunit.ServiceExporter;
@MetaInfServices(AstrixContextPlugin.class)
public class SpringModule implements AstrixContextPlugin {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(AstrixSpringContext.class, AstrixSpringContextImpl.class);
		moduleContext.bind(BeanPostProcessor.class, AstrixBeanPostProcessor.class);
		
		moduleContext.importType(ServiceExporter.class);
		
		moduleContext.export(AstrixSpringContext.class);
		moduleContext.export(BeanPostProcessor.class); // TODO: this is only exported in order to read it from AstrixFrameworkBean. Can we avoid exporting it?
	}

	@Override
	public void registerStrategies(AstrixStrategiesConfig astrixContextConfig) {
	}

}
