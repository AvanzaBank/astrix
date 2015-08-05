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
package com.avanza.astrix.context.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.beans.factory.StandardFactoryBean;

class ModuleManager implements Modules {
	
	private final Logger log = LoggerFactory.getLogger(ModuleManager.class);
	private final ConcurrentMap<Class<?>, List<ModuleInstance>> moduleByExportedType = new ConcurrentHashMap<>();
	private final List<ModuleInstance> moduleInstances = new CopyOnWriteArrayList<>();
	private final ModuleBeanPostProcessors moduleBeanPostProcessors = new ModuleBeanPostProcessors();

	void register(Module module) {
		ModuleInstance moduleInstance = new ModuleInstance(module);
		moduleInstances.add(moduleInstance);
		for (Class<?> exportedType : moduleInstance.getExports()) {
			getExportingModules(exportedType).add(moduleInstance);
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
		List<ModuleInstance> exportingModules = moduleByExportedType.get(type);
		if (exportingModules == null) {
			throw new IllegalArgumentException("Non exported type: " + type);
		}
		if (exportingModules.size() > 1) {
			log.warn("Type exported by multiple modules. Using first registered provider. Ignoring export. type={} usedModule={} ignoredModules={}",
					type.getName(),
					exportingModules.get(0).getName(),
					getModulesNames(exportingModules.subList(1, exportingModules.size())));
		}
		return exportingModules.get(0).getInstance(type);
	}
	
	@Override
	public <T> Collection<T> getAll(Class<T> type) {
		List<T> result = new ArrayList<T>();
		for (AstrixBeanKey<T> key : getBeansOfType(type)) {
			result.add(getInstance(key));
		}
		return result;
	}
	
	private List<String> getModulesNames(List<ModuleInstance> modules) {
		List<String> result = new ArrayList<String>(modules.size());
		for (ModuleInstance instance : modules) {
			result.add(instance.getName());
		}
		return result;
	}

	private <T> T getInstance(AstrixBeanKey<T> beanKey) {
		if (!beanKey.isQualified()) {
			return getInstance(beanKey.getBeanType());
		}
		List<ModuleInstance> moduleInstances = moduleByExportedType.get(beanKey.getBeanType());
		if (moduleInstances == null) {
			throw new IllegalArgumentException("Non exported bean: " + beanKey);
		}

		for (ModuleInstance moduleInstance : moduleInstances) {
			if (moduleInstance.getName().equals(beanKey.getQualifier())) {
				return moduleInstance.getInstance(beanKey.getBeanType());
			}
		}
		throw new IllegalArgumentException("Non exported bean: " + beanKey);
	}
	
	public Set<AstrixBeanKey<?>> getExportedBeanKeys() {
		Set<AstrixBeanKey<?>> result = new HashSet<>();
		for (Map.Entry<Class<?>, List<ModuleInstance>> exportedTypes : this.moduleByExportedType.entrySet()) {
			for (ModuleInstance moduleInstance : exportedTypes.getValue()) {
				result.add(AstrixBeanKey.create(exportedTypes.getKey(), moduleInstance.getName()));
			}
		}
		return result;
	}
	
	public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
		List<ModuleInstance> moduleInstances = moduleByExportedType.get(type);
		if (moduleInstances == null) {
			return Collections.emptySet();		
		}
		Set<AstrixBeanKey<T>> result = new HashSet<>();
		for (ModuleInstance moduleInstance : moduleInstances) {
			result.add(AstrixBeanKey.create(type, moduleInstance.getName()));
		}
		return result;
	}
	
	private static class ExportedModuleFactoryBean<T> implements StandardFactoryBean<T> {
		private AstrixBeanKey<T> beanKey;
		private ModuleManager moduleManager;
		
		public ExportedModuleFactoryBean(AstrixBeanKey<T> beanKey, ModuleManager moduleManager) {
			this.beanKey = Objects.requireNonNull(beanKey);
			this.moduleManager = Objects.requireNonNull(moduleManager);
		}

		@Override
		public T create(AstrixBeans beans) {
			return moduleManager.getInstance(beanKey);
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
		@Override
		public boolean lifecycled() {
			/*
			 * Exported beans are lifecycled by the module they belong to
			 */
			return false;
		}
	}
	
	public class ModuleInstance {
		
		private final ModuleInjector injector;
		private final Module module;
		private final HashSet<Class<?>> exports;
		private final HashSet<Class<?>> importedTypes;
		private final String moduleName;
		
		public ModuleInstance(Module module) {
			this.module = module;
			this.moduleName = getModuleName(module);
			this.exports = new HashSet<>();
			this.importedTypes = new HashSet<>();
			this.injector = new ModuleInjector(new AstrixFactoryBeanRegistry() {
				@Override
				public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
					// TODO
					return beanKey;
				}
				
				@Override
				public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
					if (importedTypes.contains(beanKey.getBeanType())) {
						return new ExportedModuleFactoryBean<>(beanKey, ModuleManager.this);
					}
					// Check if beanType is abstract
					if (beanKey.getBeanType().isInterface()) {
						throw new MissingBeanProviderException(beanKey, String.format("Failed to create bean=%s in module=%s", beanKey.toString(), moduleName));
					}
					return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
				}
				
				@Override
				public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
					if (!importedTypes.contains(type)) {
						return new HashSet<>();
					}
					return ModuleManager.this.getBeansOfType(type);
				}
			});
			this.injector.registerBeanPostProcessor(moduleBeanPostProcessors);
			this.module.prepare(new ModuleContext() {
				@Override
				public <T> void bind(Class<T> type, Class<? extends T> providerType) {
					injector.bind(AstrixBeanKey.create(type), providerType);
				}
				@Override
				public <T> void bind(Class<T> type, T provider) {
					injector.bind(AstrixBeanKey.create(type), provider);
				}
				@Override
				public void export(Class<?> type) {
					if (!type.isInterface()) {
						throw new IllegalArgumentException(String.format("Its only allowed to export interface types. module=%s exportedType=%s", moduleName, type));
					}
					exports.add(type);
				}
				@Override
				public <T> void importType(final Class<T> type) {
					if (!type.isInterface()) {
						throw new IllegalArgumentException(String.format("Its only allowed to interface types. module=%s importedType=%s", moduleName, type));
					}
					importedTypes.add(type);
				}
			});
			
		}

		private String getModuleName(Module module) {
			if (module instanceof NamedModule) {
				return NamedModule.class.cast(module).name();
			}
			return module.getClass().getName();
		}

		public String getName() {
			return this.moduleName;
		}
		
		public Set<Class<?>> getExports() {
			return this.exports;
		}

		public <T> T getInstance(Class<T> type) {
			if (!getExports().contains(type)) {
				throw new IllegalArgumentException("Module does not export type=" + type);
			}
			return injector.getBean(type);
		}

		public void destroy() {
			this.injector.destroy();
		}
	}

	@Override
	@PreDestroy
	public void destroy() {
		for (ModuleInstance moduleInstance : this.moduleInstances) {
			moduleInstance.destroy();
		}
	}

	public void registerBeanPostProcessor(AstrixBeanPostProcessor beanPostProcessor) {
		this.moduleBeanPostProcessors.add(beanPostProcessor);
	}
	
	private static class ModuleBeanPostProcessors implements AstrixBeanPostProcessor {
		private final List<AstrixBeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();		
		@Override
		public void postProcess(Object bean, AstrixBeans astrixBeans) {
			for (AstrixBeanPostProcessor beanPostProcessor : beanPostProcessors) {
				beanPostProcessor.postProcess(bean, astrixBeans);
			}
		}
		
		void add(AstrixBeanPostProcessor beanPostProcessor) {
			this.beanPostProcessors.add(beanPostProcessor);
		}
	}

}
