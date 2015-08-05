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
package com.avanza.astrix.ft;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.StrategyContext;
import com.avanza.astrix.modules.StrategyContextPreparer;

@MetaInfServices(AstrixContextPlugin.class)
public class HystrixAstrixContextPlugin implements AstrixContextPlugin {
	
	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
		astrixContextConfig.registerModule(this);
		
		// Exposes strategy: HystrixCommandNamingStrategy
		astrixContextConfig.registerDefaultStrategy(HystrixCommandNamingStrategy.class, DefaultHystrixCommandNamingStrategy.class);
		
		astrixContextConfig.registerStrategy(BeanFaultToleranceProxyStrategy.class, HystrixFaultToleranceProxyProvider.class, 
				new StrategyContextPreparer() {
					@Override
					public void prepare(StrategyContext context) {
						context.importType(BeanFaultToleranceFactory.class);
					}
				});
		
	}
	
	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(BeanFaultToleranceFactory.class, BeanFaultToleranceFactoryImpl.class);
		
		moduleContext.importType(HystrixCommandNamingStrategy.class); // strategy
		moduleContext.importType(BeanConfigurations.class);
		
		moduleContext.export(BeanFaultToleranceFactory.class);
	}
	@Override
	public String name() {
		return "hystrix-fault-tolerance";
	}
	
}
