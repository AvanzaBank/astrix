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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.inject.ClassConstructorFactoryBean;
import com.avanza.astrix.core.util.ReflectionUtil;

public class PluginManager {
	
	private final ConcurrentMap<Class<?>, PluginInstance> pluginByExportedType = new ConcurrentHashMap<>();
	private final List<PluginInstance> pluginInstances = new CopyOnWriteArrayList<>();

	public void register(AstrixPlugin plugin) {
		PluginInstance pluginInstance = new PluginInstance(plugin);
		for (Class<?> exportedType : pluginInstance.getExports()) {
			pluginByExportedType.put(exportedType, pluginInstance);
			pluginInstances.add(pluginInstance);
		}
	}

	public <T> T getPluginInstance(Class<T> type) {
		PluginInstance pluginInstance = pluginByExportedType.get(type);
		if (pluginInstance == null) {
			throw new IllegalArgumentException("Non exported type: " + type);
		}
		return pluginInstance.getInstance(type);
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
	
	public static class ExportedPluginFactoryBean<T> implements StandardFactoryBean<T> {
		private AstrixBeanKey<T> beanKey;
		private PluginManager pluginManager;
		
		public ExportedPluginFactoryBean(AstrixBeanKey<T> beanKey, PluginManager pluginManager) {
			this.beanKey = beanKey;
			this.pluginManager = pluginManager;
		}

		@Override
		public T create(AstrixBeans beans) {
			/*
			 * Wraps retrieved plugin instance in proxy in order to avoid
			 * that the retrieved plugin instance receives lifecycle callbacks
			 * from the Injector used by the importing plugin instance.
			 */
			return ReflectionUtil.newProxy(beanKey.getBeanType(), new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return pluginManager.getPluginInstance(beanKey.getBeanType());
				}
			});
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
		
	}
	
	public class PluginInstance {
		
		private final AstrixInjector injector;
		private final AstrixPlugin plugin;
		private final HashSet<Class<?>> exports;
		private final ClassConstructorFactoryBeanRegistry imports = new ClassConstructorFactoryBeanRegistry();
		
		public PluginInstance(AstrixPlugin plugin) {
			this.plugin = plugin;
			this.exports = new HashSet<>();
			this.injector = new AstrixInjector(imports);
			this.plugin.prepare(new PluginContext() {
				@Override
				public <T> void bind(Class<T> type, Class<? extends T> providerType) {
					injector.bind(type, providerType);
				}
				@Override
				public <T> void bind(Class<T> type, T provider) {
					injector.bind(type, provider);
				}
				@Override
				public void export(Class<?> pluginType) {
					exports.add(pluginType);
				}
				@Override
				public <T> void importPlugin(final Class<T> pluginType) {
					injector.bind(pluginType, new ExportedPluginFactoryBean<>(AstrixBeanKey.create(pluginType), PluginManager.this));
				}
			});
		}

		public Set<Class<?>> getExports() {
			return this.exports;
		}

		public <T> T getInstance(Class<T> type) {
			if (!getExports().contains(type)) {
				throw new IllegalArgumentException("Plugin does not export type=" + type);
			}
			return injector.getBean(type);
		}

		public void destroy() {
			this.injector.destroy();
		}
	}

	public void destroy() {
		for (PluginInstance pluginInstance : this.pluginInstances) {
			pluginInstance.destroy();
		}
	}

	public void autoDiscover() {
		List<AstrixPlugin> plugins = PluginDiscovery.discoverAllPlugins(AstrixPlugin.class);
		for (AstrixPlugin plugin : plugins) {
			register(plugin);
		}
	}

}
