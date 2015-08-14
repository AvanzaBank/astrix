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

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.NamedModule;

public class FaultToleranceModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(BeanFaultToleranceFactory.class, BeanFaultToleranceFactoryImpl.class);
		
		moduleContext.importType(FaultToleranceSpi.class);
		moduleContext.importType(HystrixCommandNamingStrategy.class);
		moduleContext.importType(BeanConfigurations.class);
		moduleContext.importType(AstrixConfig.class);
		
		moduleContext.export(BeanFaultToleranceFactory.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
