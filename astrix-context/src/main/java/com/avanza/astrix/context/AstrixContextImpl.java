/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PreDestroy;

import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.ObjectCache;
import com.avanza.astrix.config.DynamicConfig;
/**
 * An AstrixContextImpl is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContextImpl providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixContextImpl implements Astrix, AstrixContext {
	
	private final AstrixPlugins plugins;
	private final SimpleAstrixFactoryBeanRegistry beanFactoryRegistry = new SimpleAstrixFactoryBeanRegistry();
	private AstrixApiProviderPlugins apiProviderPlugins;
	private final ObjectCache objectCache = new ObjectCache();
	private final AstrixBeanFactory beanFactory;
	private final DynamicConfig dynamicConfig;
	
	public AstrixContextImpl(DynamicConfig dynamicConfig) {
		this.dynamicConfig = dynamicConfig;
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.beanFactory = objectCache.create(AstrixBeanFactory.class, new AstrixBeanFactory(beanFactoryRegistry));
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	<T> void registerBeanFactory(AstrixFactoryBean<T> beanFactory) {
		injectDependencies(beanFactory);
		this.beanFactoryRegistry.registerFactory(beanFactory);
	}
	
	@Override
	@PreDestroy
	public void destroy() {
		this.objectCache.destroy();
	}
	
	private void injectDependencies(Object object) {
		injectAwareDependencies(object);
		injectAstrixClasses(object);
	}

	private void injectAstrixClasses(Object object) {
		for (Method m : object.getClass().getMethods()) {
			if (!m.isAnnotationPresent(AstrixInject.class)) {
				continue;
			}
			if (m.getParameterTypes().length == 0) {
				throw new IllegalArgumentException(String.format("@AstrixInject annotated methods must accept at least one dependency. Class: %s, method: %s"
						, object.getClass().getName()
						, m.getName()));
			}
			Object[] deps = new Object[m.getParameterTypes().length];
			for (int argIndex = 0; argIndex < deps.length; argIndex++) {
				Class<?> dep = m.getParameterTypes()[argIndex];
				deps[argIndex] = getInstance(dep);
			}
			try {
				m.invoke(object, deps);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Failed to inject dependencies into Astrix managed component: " + object.getClass().getName(), e);
			}
		}
	}

	private void injectAwareDependencies(Object object) {
		if (object instanceof AstrixBeansAware) {
			injectBeanDependencies((AstrixBeansAware)object);
		}
		if (object instanceof AstrixPluginsAware) {
			AstrixPluginsAware.class.cast(object).setPlugins(getPlugins());
		}
		if (object instanceof AstrixConfigAware) {
			AstrixConfigAware.class.cast(object).setConfig(this.dynamicConfig);
		}
	}
	
	private void injectBeanDependencies(final AstrixBeansAware beanDependenciesAware) {
		beanDependenciesAware.setAstrixBeans(new AstrixBeans() {
			@Override
			public <T> T getBean(AstrixBeanKey<T> beanKey) {
				return AstrixContextImpl.this.getBean(beanKey);
			}
		});
	}

	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return beanFactory.getBean(AstrixBeanKey.create(beanType, qualifier));
	}
	
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return beanFactory.getBean(beanKey);
	}

	public AstrixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		AstrixBeanKey<T> beanKey = AstrixBeanKey.create(beanType, qualifier);
		T bean = beanFactory.getBean(beanKey);
		for (AstrixBeanKey<?> dependencyKey : beanFactory.getDependencies(beanKey)) {
			Object dependency = beanFactory.getBean(dependencyKey);
			waitForBeanToBeBound(dependency, timeoutMillis);
		}
		waitForBeanToBeBound(bean, timeoutMillis);
		return bean;
	}
	private void waitForBeanToBeBound(Object bean, long timeoutMillis) throws InterruptedException {
		if (bean instanceof StatefulAstrixBean) {
			StatefulAstrixBean.class.cast(bean).waitUntilBound(timeoutMillis);
		}
	}


	List<AstrixServiceBeanDefinition> getExportedServices(AstrixApiDescriptor apiDescriptor) {
		return apiProviderPlugins.getExportedServices(apiDescriptor);
	}

	void setApiProviderPlugins(AstrixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}

	public <T> T getPlugin(Class<T> pluginType) {
		return getPlugins().getPlugin(pluginType);
	}
	
	/**
	 * Returns an instance of an internal framework class.
	 * @param classType
	 * @return
	 */
	public final <T> T getInstance(final Class<T> classType) {
		// We allow injecting an instance of ObjectCache, hence this
		// check.
		if (classType.equals(ObjectCache.class)) {
			return classType.cast(objectCache);
		}
		if (classType.equals(AstrixContextImpl.class)) {
			return classType.cast(this);
		}
		if (classType.equals(AstrixApiProviderPlugins.class)) {
			return classType.cast(this.apiProviderPlugins);
		}
		return this.objectCache.getInstance(classType, new ObjectCache.ObjectFactory<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T create() throws Exception {
				T result =  (T) classType.newInstance();
				injectDependencies(result);
				return result;
			}
		});
	}
	
	public DynamicConfig getConfig() {
		return dynamicConfig;
	}
	
	String getCurrentSubsystem() {
		return this.dynamicConfig.getStringProperty(AstrixSettings.SUBSYSTEM_NAME, null).get();
	}

}
