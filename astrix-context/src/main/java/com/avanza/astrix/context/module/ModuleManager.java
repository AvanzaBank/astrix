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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.core.util.ReflectionUtil;

public class ModuleManager {
	
	private final Logger log = LoggerFactory.getLogger(ModuleManager.class);
	private final ConcurrentMap<Class<?>, List<ModuleInstance>> moduleByExportedType = new ConcurrentHashMap<>();
	private final List<ModuleInstance> moduleInstances = new CopyOnWriteArrayList<>();

	public void register(Module module) {
		ModuleInstance moduleInstance = new ModuleInstance(module);
		moduleInstances.add(moduleInstance);
		for (Class<?> exportedType : moduleInstance.getExports()) {
			List<ModuleInstance> modules = moduleByExportedType.get(exportedType);
			if (modules == null) {
				modules = new LinkedList<ModuleManager.ModuleInstance>();
				moduleByExportedType.put(exportedType, modules);
			}
			modules.add(moduleInstance);
//			ModuleInstance alreadyRegisteredProvider = moduleByExportedType.putIfAbsent(exportedType, moduleInstance);
//			if (alreadyRegisteredProvider != null) {
//				log.warn("Type already exported by another module. Ignoring export. type={} usedModule={} ignoredModule={}", 
//						exportedType.getName(),
//						alreadyRegisteredProvider.getName(),
//						moduleInstance.getName());
//			}
		}
	}

	public <T> T getInstance(Class<T> type) {
		List<ModuleInstance> moduleInstances = moduleByExportedType.get(type);
		if (moduleInstances == null) {
			throw new IllegalArgumentException("Non exported type: " + type);
		}
		if (moduleInstances.size() > 1) {
//			log.warn("Type already exported by another module. Ignoring export. type={} usedModule={} ignoredModule={}", 
//			exportedType.getName(),
//			alreadyRegisteredProvider.getName(),
//			moduleInstance.getName());			
		}
		return moduleInstances.get(0).getInstance(type);
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
			/*
			 * Wraps retrieved module instance in proxy in order to avoid
			 * that the retrieved module instance receives lifecycle callbacks
			 * from the Injector used by the importing module instance.
			 */
			final T instance = moduleManager.getInstance(beanKey);
			return ReflectionUtil.newProxy(beanKey.getBeanType(), new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//					return moduleManager.getInstance(beanKey);
					return method.invoke(instance, args);
				}
			});
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
		
	}
	
	public class ModuleInstance {
		
		private final ModuleInjector injector;
		private final Module module;
		private final HashSet<Class<?>> exports;
		private final HashSet<Class<?>> importedBeans;
		private final HashSet<Class<?>> importedTypes;
		
		public ModuleInstance(Module module) {
			this.module = module;
			this.exports = new HashSet<>();
			this.importedBeans = new HashSet<>();
			this.importedTypes = new HashSet<>();
			this.injector = new ModuleInjector(new AstrixFactoryBeanRegistry() {
				@Override
				public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
					// TODO
					return beanKey;
				}
				
				@Override
				public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
					if (importedBeans.contains(beanKey.getBeanType()) || importedTypes.contains(beanKey.getBeanType())) {
						return new ExportedModuleFactoryBean<>(beanKey, ModuleManager.this);
					}
					// Check if beanType is abstract
					if (beanKey.getBeanType().isInterface()) {
						throw new IllegalArgumentException(beanKey.toString());
					}
					return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
				}
				
				@Override
				public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
					if (!importedTypes.contains(type)) {
						return Collections.emptySet();
					}
					return ModuleManager.this.getBeansOfType(type);
				}
			});
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
				public void export(Class<?> moduleType) {
					exports.add(moduleType);
				}
				@Override
				public <T> void importType(final Class<T> moduleType) {
					injector.bind(AstrixBeanKey.create(moduleType), new ExportedModuleFactoryBean<>(AstrixBeanKey.create(moduleType), ModuleManager.this));
				}
				
				@Override
				public <T> void importAllOfType(Class<T> type) {
					importedTypes.add(type);
				}
				
				@Override
				public <T> void bind(Class<T> type, Class<? extends T> providerType, String qualifier) {
					injector.bind(AstrixBeanKey.create(type, qualifier), providerType);
				}
				@Override
				public <T> void bind(Class<T> type, T provider, String qualifier) {
					injector.bind(AstrixBeanKey.create(type, qualifier), provider);
					
				}
			});
			
		}

		public String getName() {
			return module.getClass().getName();
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

	public void destroy() {
		for (ModuleInstance moduleInstance : this.moduleInstances) {
			moduleInstance.destroy();
		}
	}

	public void autoDiscover() {
		List<Module> modules = ModuleDiscovery.loadModules();
		for (Module module : modules) {
			register(module);
		}
	}

}
