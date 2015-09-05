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
package com.avanza.astrix.modules;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModulesConfigurer {
	
	private final ConcurrentMap<String, Module> moduleNames = new ConcurrentHashMap<>();
	private final List<Module> modules = new LinkedList<>();
	private final ConcurrentMap<Class<?>, StrategyProvider<?>> strategyProviders = new ConcurrentHashMap<>();
	private final List<ModuleInstancePostProcessor> postProcessors = new CopyOnWriteArrayList<>();
	
	public void register(Module module) {
		String moduleName = getModuleName(module);
		Module conflictingModule = moduleNames.putIfAbsent(moduleName, module);
		if (conflictingModule != null) {
			throw new ModuleNameConflict(moduleName, module, conflictingModule);
		}
		modules.add(module);
	}

	private String getModuleName(Module module) {
		return module.name();
	}
	
	public void register(StrategyProvider<?> strategy) {
		strategyProviders.put(strategy.getStrategyType(), strategy);
	}
	
	public void registerDefault(StrategyProvider<?> strategy) {
		strategyProviders.putIfAbsent(strategy.getStrategyType(), strategy);
	}
	
	public void registerBeanPostProcessor(
			ModuleInstancePostProcessor beanPostProcessor) {
		postProcessors.add(beanPostProcessor);
	}

	public Modules configure() {
		ModuleManager moduleManager = new ModuleManager();
		for (Module module : modules) {
			moduleManager.register(module);
		}
		for (StrategyProvider<?> strategyProvider : strategyProviders.values()) {
			moduleManager.register(strategyProvider);
		}
		for (ModuleInstancePostProcessor beanPostProcessor : postProcessors) {
			moduleManager.registerBeanPostProcessor(beanPostProcessor);
		}
		return moduleManager;
	}
	
}
