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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.modules.ObjectCache.ObjectFactory;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ModuleInjector {
	
	// TODO: make package private?
	
	private final ObjectCache objectCache = new ObjectCache();
	private final ConcurrentMap<Class<?>, Class<?>> beanBindings = new ConcurrentHashMap<>();
	private final Set<Class<?>> imports = new HashSet<>();
	private final Set<Class<?>> exports = new HashSet<>();
	private final String moduleName;
	private final ModuleInstancePostProcessors postProcessors = new ModuleInstancePostProcessors();
	
	public ModuleInjector(String moduleName) {
		this.moduleName = moduleName;
	}

	public void registerBeanPostProcessor(ModuleInstancePostProcessor beanPostProcessor) {
		this.postProcessors.add(beanPostProcessor);
	}
	
	public <T> void bind(Class<T> type, Class<? extends T> providerType) {
		this.beanBindings.put(type, providerType);
	}

	public <T> void bind(Class<T> type, final T provider) {
		beanBindings.put(type, provider.getClass());
		objectCache.create(provider.getClass(), provider);
	}
	
	public <T> T getBean(Class<T> type, ImportedDependencies importedDependencies) {
		if (!exports.contains(type)) {
			throw new IllegalArgumentException("Non exported bean: " + type);
		}
		return new CirucularDependenciesAwareCreation(importedDependencies).create(type);
	}
	
	public class CirucularDependenciesAwareCreation {
		
		private final Stack<Class<?>> constructionStack = new Stack<>();
		private final ImportedDependencies importedDependencies;
		
		public CirucularDependenciesAwareCreation(ImportedDependencies importedDependencies) {
			this.importedDependencies = importedDependencies;
		}

		public <T> T create(Class<T> type) {
			final Class<T> resolvedType = resolveBean(type);
			return objectCache.getInstance(resolvedType, new ObjectFactory<T>() {
				@Override
				public T create() throws Exception {
					return doCreate(resolvedType);
				}
			});
		}
		
		public <T> Class<T> resolveBean(Class<T> type) {
			Class<?> boundType = beanBindings.get(type);
			if (boundType != null) {
				return (Class<T>) boundType;
			}
			if (Modifier.isAbstract(type.getModifiers()) || type.isInterface()) {
				throw new MissingBeanBinding(moduleName, type, constructionStack);
			}
			return type;
		}
		
		private <T> T doCreate(final Class<T> type) {
			try {
				if (constructionStack.contains(type)) {
//				throw new CircularDependency(constructionStack, moduleName);
					throw new CircularDependency();
				}
				constructionStack.add(type);
				final ClassConstructorFactory<T> factory = new ClassConstructorFactory<T>(type);
				T result = factory.create(new Dependencies() {
					@Override
					public <E> Collection<E> getAll(Class<E> type) {
						List<E> result = new ArrayList<>();
						if (ModuleInjector.this.imports.contains(type)) {
							result.addAll(importedDependencies.getAll(type));
						}
						if (ModuleInjector.this.beanBindings.containsKey(type)) {
							result.add(create(type));
						}
						return result;
					}
					@Override
					public <E> E get(Class<E> type) {
						if (ModuleInjector.this.imports.contains(type)) {
							return importedDependencies.get(type);
						}
						return create(type); 
					}
				});
				postProcessors.postProcess(result);
				constructionStack.pop();
				return result;
			} catch (ModulesConfigurationException configurationException) {
				configurationException.addToDependencyTrace(type, moduleName);
				throw configurationException;
			}
		}
	}

	public void destroy() {
		this.objectCache.destroy();
	}

	public void addExport(Class<?> exportedBean) {
		this.exports.add(exportedBean);
	}
	
	public void addImport(Class<?> importedBean) {
		this.imports.add(importedBean);
	}

	public Set<Class<?>> getExports() {
		return this.exports;
	}

}
