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
package com.avanza.astrix.context.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.inject.ClassConstructorFactoryBean;
import com.avanza.astrix.core.util.ReflectionUtil;

public class ModuleManager {
	
	private final Logger log = LoggerFactory.getLogger(ModuleManager.class);
	private final ConcurrentMap<Class<?>, ModuleInstance> moduleByExportedType = new ConcurrentHashMap<>();
	private final List<ModuleInstance> moduleInstances = new CopyOnWriteArrayList<>();

	public void register(Module module) {
		ModuleInstance moduleInstance = new ModuleInstance(module);
		moduleInstances.add(moduleInstance);
		for (Class<?> exportedType : moduleInstance.getExports()) {
			ModuleInstance alreadyRegisteredProvider = moduleByExportedType.putIfAbsent(exportedType, moduleInstance);
			if (alreadyRegisteredProvider != null) {
				log.warn("Type already exported by another module. Ignoring export. type={} usedModule={} ignoredModule={}", 
						exportedType.getName(),
						alreadyRegisteredProvider.getName(),
						moduleInstance.getName());
			}
		}
	}

	public <T> T getInstance(Class<T> type) {
		ModuleInstance moduleInstance = moduleByExportedType.get(type);
		if (moduleInstance == null) {
			throw new IllegalArgumentException("Non exported type: " + type);
		}
		return moduleInstance.getInstance(type);
	}
	
	public static class ClassConstructorFactoryBeanRegistry implements AstrixFactoryBeanRegistry {
		@Override
		public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
			Class<T> beanType = beanKey.getBeanType();
			if (Modifier.isAbstract(beanType.getModifiers()) || beanType.isInterface()) {
				throw new MissingBeanProviderException(beanKey);
			}
			return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
		}
		@Override
		public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
			return beanKey;
		}
		@Override
		public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
			return new HashSet<>();
		}
	}
	
	public static class ExportedModuleFactoryBean<T> implements StandardFactoryBean<T> {
		private AstrixBeanKey<T> beanKey;
		private ModuleManager moduleManager;
		
		public ExportedModuleFactoryBean(AstrixBeanKey<T> beanKey, ModuleManager moduleManager) {
			this.beanKey = beanKey;
			this.moduleManager = moduleManager;
		}

		@Override
		public T create(AstrixBeans beans) {
			/*
			 * Wraps retrieved module instance in proxy in order to avoid
			 * that the retrieved module instance receives lifecycle callbacks
			 * from the Injector used by the importing module instance.
			 */
			return ReflectionUtil.newProxy(beanKey.getBeanType(), new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return moduleManager.getInstance(beanKey.getBeanType());
				}
			});
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
		
	}
	
	public class ModuleInstance {
		
		private final AstrixInjector injector;
		private final Module module;
		private final HashSet<Class<?>> exports;
		private final ClassConstructorFactoryBeanRegistry imports = new ClassConstructorFactoryBeanRegistry();
		
		public ModuleInstance(Module module) {
			this.module = module;
			this.exports = new HashSet<>();
			this.injector = new AstrixInjector(imports);
			this.module.prepare(new ModuleContext() {
				@Override
				public <T> void bind(Class<T> type, Class<? extends T> providerType) {
					injector.bind(type, providerType);
				}
				@Override
				public <T> void bind(Class<T> type, T provider) {
					injector.bind(type, provider);
				}
				@Override
				public void export(Class<?> moduleType) {
					exports.add(moduleType);
				}
				@Override
				public <T> void importPlugin(final Class<T> moduleType) {
					injector.bind(moduleType, new ExportedModuleFactoryBean<>(AstrixBeanKey.create(moduleType), ModuleManager.this));
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
