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
package com.avanza.astrix.ft.hystrix;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.context.AstrixStrategiesConfig;
import com.avanza.astrix.modules.ModuleContext;


public class HystrixModule implements AstrixContextPlugin {
	
	@Override
	public void registerStrategies(AstrixStrategiesConfig strategiesConfig) {
		strategiesConfig.registerStrategy(BeanFaultToleranceFactorySpi.class, HystrixFaultToleranceFactory.class, (context) -> {
			context.importType(AstrixConfig.class);
			context.importType(HystrixCommandNamingStrategy.class);
			context.importType(AstrixTraceProvider.class);
		});
	}

	@Override
	public void prepare(ModuleContext moduleContext) {
	}
	
}
