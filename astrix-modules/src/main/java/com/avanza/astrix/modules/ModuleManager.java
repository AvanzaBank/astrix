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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleManager implements Modules {
	
	private final Logger log = LoggerFactory.getLogger(ModuleManager.class);
	private final ConcurrentMap<Class<?>, List<ModuleInstance>> moduleByExportedType = new ConcurrentHashMap<>();
	private final List<ModuleInstance> moduleInstances = new CopyOnWriteArrayList<>();
	private final ModuleInstancePostProcessors globalModuleBeanPostProcessors = new ModuleInstancePostProcessors();

	void register(Module module) {
		ModuleInstance moduleInstance = new ModuleInstance(module, globalModuleBeanPostProcessors);
		moduleInstances.add(moduleInstance);
		for (Class<?> exportedBean : moduleInstance.getExports()) {
			getExportingModules(exportedBean).add(moduleInstance);
		}
	}

	private List<ModuleInstance> getExportingModules(Class<?> exportedType) {
		List<ModuleInstance> modules = moduleByExportedType.get(exportedType);
		if (modules == null) {
			modules = new LinkedList<ModuleManager.ModuleInstance>();
			moduleByExportedType.put(exportedType, modules);
		}
		return modules;
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return new CircularModuleDependenciesAwareCreation().get(type);
	}
	
	@Override
	public <T> Collection<T> getAll(Class<T> type) {
		return new CircularModuleDependenciesAwareCreation().getAll(type);
	}
	
	private List<String> getModulesNames(List<ModuleInstance> modules) {
		List<String> result = new ArrayList<String>(modules.size());
		for (ModuleInstance instance : modules) {
			result.add(instance.getName());
		}
		return result;
	}
	
	public static class ModuleInstance {

		private final ModuleInjector moduleInjector;
		private final String moduleName;
		
		public ModuleInstance(Module module, ModuleInstancePostProcessor moduleInstancePostProcessor) {
			this.moduleName = getModuleName(module);
			this.moduleInjector = new ModuleInjector(moduleName);
			this.moduleInjector.registerBeanPostProcessor(moduleInstancePostProcessor);
			module.prepare(new ModuleContext() {
				@Override
				public <T> void bind(Class<T> type, Class<? extends T> providerType) {
					moduleInjector.bind(type, providerType);
				}
				@Override
				public <T> void bind(Class<T> type, T provider) {
					moduleInjector.bind(type, provider);
				}
				@Override
				public void export(Class<?> type) {
					if (!type.isInterface()) {
						throw new IllegalArgumentException(String.format("Its only allowed to export interface types. module=%s exportedType=%s", moduleName, type));
					}
					moduleInjector.addExport(type);
				}
				@Override
				public <T> void importType(final Class<T> type) {
					if (!type.isInterface()) {
						throw new IllegalArgumentException(String.format("Its only allowed to import interface types. module=%s importedType=%s", moduleName, type));
					}
					moduleInjector.addImport(type);
				}
			});
			
		}

		private String getModuleName(Module module) {
			return module.name();
		}

		public String getName() {
			return this.moduleName;
		}
		
		public Set<Class<?>> getExports() {
			return this.moduleInjector.getExports();
		}

		public <T> T getInstance(final Class<T> type, ImportedDependencies importedDependencies) {
			return this.moduleInjector.getBean(type, importedDependencies);
		}

		public void destroy() {
			this.moduleInjector.destroy();
		}
	}

	@Override
	@PreDestroy
	public void destroy() {
		for (ModuleInstance moduleInstance : this.moduleInstances) {
			moduleInstance.destroy();
		}
	}
	
	public static class CreationFrame {
		
		private ModuleInstance module;
		private Class<?> type;
		public CreationFrame(ModuleInstance module, Class<?> type) {
			this.module = module;
			this.type = type;
		}
		@Override
		public int hashCode() {
			return Objects.hash(module, type);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CreationFrame other = (CreationFrame) obj;
			return Objects.equals(module, other.module)
					&& Objects.equals(type, other.type);
		}
		
	}
	
	public class CircularModuleDependenciesAwareCreation {
		
		private final Stack<CreationFrame> creationStack = new Stack<>();
		
		public <T> T get(final Class<T> type) {
			List<ModuleInstance> exportingModules = moduleByExportedType.get(type);
			if (exportingModules == null) {
				throw new MissingProvider(type);
			}
			if (exportingModules.size() > 1) {
				log.warn("Type exported by multiple modules. Using first registered provider. Ignoring export. type={} usedModule={} ignoredModules={}",
						type.getName(),
						exportingModules.get(0).getName(),
						getModulesNames(exportingModules.subList(1, exportingModules.size())));
			}
			ModuleInstance exportingModule = exportingModules.get(0);
			return getInstance(type, exportingModule);
		}

		public <T> Collection<T> getAll(Class<T> type) {
			List<T> result = new ArrayList<>();
			List<ModuleInstance> exportingModules = moduleByExportedType.get(type);
			if (exportingModules == null) {
				return result;
			}
			for (ModuleInstance exportingModule : exportingModules) {
				result.add(getInstance(type, exportingModule));
			}
			return result;
		}
		
		private <T> T getInstance(final Class<T> type, ModuleInstance exportingModule) {
			CreationFrame creationFrame = new CreationFrame(exportingModule, type);
			if (creationStack.contains(creationFrame)) {
				CircularDependency circularDependency = new CircularDependency();
				circularDependency.addToDependencyTrace(type, exportingModule.getName());
				throw circularDependency;
			}
			creationStack.add(creationFrame);
			T result = exportingModule.getInstance(type, new ImportedDependencies() {
				@Override
				public <D> Collection<D> getAll(Class<D> type) {
					return CircularModuleDependenciesAwareCreation.this.getAll(type);
				}
				
				@Override
				public <D> D get(Class<D> type) {
					return CircularModuleDependenciesAwareCreation.this.get(type);
				}
			});
			creationStack.pop();
			return result;
		}
		
	}

	public void registerBeanPostProcessor(ModuleInstancePostProcessor beanPostProcessor) {
		this.globalModuleBeanPostProcessors.add(beanPostProcessor);
	}

}
