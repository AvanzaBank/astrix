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
package com.avanza.astrix.test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.config.Setting;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AstrixTestContext implements AstrixContext {

	private final ProxiedServiceRegistry serviceRegistry;
	private final AstrixContext context;
	private final ConcurrentMap<AstrixBeanKey<?>, ProviderProxy<?>> providers = new ConcurrentHashMap<>();
	private final TestApis testApis;

	/**
	 * @deprecated Please use {@link #AstrixTestContext(MapConfigSource, Class[])}
	 */
	@Deprecated
	@SafeVarargs
	public AstrixTestContext(Class<? extends TestApi>... testApis) {
		this(new MapConfigSource(), testApis);
	}

	@SafeVarargs
	public AstrixTestContext(
			MapConfigSource configSource,
			Class<? extends TestApi>... testApis
	) {
		this.serviceRegistry = new ProxiedServiceRegistry(configSource);
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setConfig(DynamicConfig.create(this.serviceRegistry));
		this.context = configurer.configure();
		this.testApis = new TestApis(this, testApis);
	}

	/**
	 * The configSourceId might be used to retrieve the ConfigSource instance
	 * using {@link GlobalConfigSourceRegistry#getConfigSource(String)}
	 *
	 * @return the configSourceId for the associated ConfigSource.
	 */
	public String getConfigSourceId() {
		return serviceRegistry.getConfigSourceId();
	}

	/**
	 * @return the serviceUri for the associated service-registry.
	 */
	public String getServiceRegistryUri() {
		return serviceRegistry.getServiceUri();
	}

	@Override
	public <T> T waitForBean(Class<T> type, long timeoutMillis) throws InterruptedException {
		return this.context.waitForBean(type, timeoutMillis);
	}

	@Override
	public <T> T waitForBean(Class<T> type, String qualifier, long timeoutMillis) throws InterruptedException {
		return this.context.waitForBean(type, qualifier, timeoutMillis);
	}

	@Override
	public <T> T getBean(Class<T> serviceBean) {
		return this.context.getBean(serviceBean);
	}

	@Override
	public <T> T getBean(Class<T> serviceBean, String qualifier) {
		return this.context.getBean(serviceBean, qualifier);
	}

	@Override
	public void destroy() {
		this.context.destroy();
	}

	@Override
	public void close() throws Exception {
		this.context.close();
	}

	/**
	 * @see AstrixRuleContext#registerProxy(Class)
	 *
	 * @param service - The qualified bean type to register a proxy for
	 */
    public <T> void registerProxy(Class<T> service) {
    	setProxyState(service, null);
    }

	/**
	 * @see AstrixRuleContext#registerProxy(Class)
	 *
	 * @param service - The qualified bean type to register a proxy for
	 * @param qualifier - The qualifier of the bean type to register a proxy for
	 */
    public <T> void registerProxy(Class<T> service, String qualifier) {
        setProxyState(service, qualifier, null);
    }

	<T> void set(Setting<T> setting, T value) {
		serviceRegistry.set(setting, value);
	}

	/**
     * Sets the proxy state for a given proxy in the service registry. If no proxy has bean registered
     * before it will be created with the given initial state,
     * see {@link AstrixRuleContext#registerProxy(Class)} for more details.
     *
     * When a service-provider is proxied it allows fast switching of the given provider between
     * different test-runs, without restarting the entire test environment.
	 *
	 * @param type - The api to register a provider for.
	 * @param mock - The instance to delegate all invocations to the given api to. Might be null in which case a ServiceUnavailableException will be thrown when the service is invoked.
	 */
	public <T> void setProxyState(final Class<T> type, final T mock) {
        @SuppressWarnings("unchecked")
        ProviderProxy<T> proxy = (ProviderProxy<T>) providers.computeIfAbsent(AstrixBeanKey.create(type), key -> {
			ProviderProxy<T> providerProxy = new ProviderProxy<>(type, mock);
			serviceRegistry.registerProvider(type, providerProxy.newProxy());
			return providerProxy;
		});
        proxy.set(mock);
	}


    /**
     * Sets the proxy state for a given proxy in the service registry. If no proxy has bean registered
     * before it will be created with the given initial state,
     * see {@link AstrixRuleContext#registerProxy(Class)} for more details.
     *
     * When a service-provider is proxied it allows fast switching of the given provider between
     * different test-runs, without restarting the entire test environment.
     *
     * @param type - The api to register a provider for.
     * @param qualifier - The qualifier for the service bean to register a provider for
     * @param mock - The instance to delegate all invocations to the given api to. Might be null in which case a ServiceUnavailableException will be thrown when the service is invoked.
     *
     */
    public <T> void setProxyState(final Class<T> type, final String qualifier, final T mock) {
		ProviderProxy<T> proxy = getOrCreateProviderProxy(type, qualifier);
        proxy.set(mock);
    }

	private <T> ProviderProxy<T> getOrCreateProviderProxy(final Class<T> type, final String qualifier) {
		@SuppressWarnings("unchecked")
        ProviderProxy<T> proxy = (ProviderProxy<T>) providers.computeIfAbsent(AstrixBeanKey.create(type,qualifier), key -> {
		    ProviderProxy<T> providerProxy = new ProviderProxy<>(type, null);
		    serviceRegistry.registerProvider(type, qualifier, providerProxy.newProxy());
		    return providerProxy;
		});
		return proxy;
	}

	public void resetProxies() {
		providers.values().forEach(providerProxy -> providerProxy.set(null));
	}

	public AstrixContext getAstrixContext() {
		return context;
	}

	public <T extends TestApi> T getTestApi(Class<T> testApi) {
		return this.testApis.getTestApi(testApi);
	}

	public void resetTestApis() {
		this.testApis.reset();
	}

	public void setConfigurationProperty(String settingName, String value) {
		this.serviceRegistry.set(settingName, value);
	}

	private static class ProviderProxy<T> implements InvocationHandler {
		private volatile T target;
		private final Class<T> type;

		public ProviderProxy(Class<T> type, T target) {
			this.type = type;
			this.target = target;
		}

		public void set(T provider) {
			this.target = provider;
		}

		public T newProxy() {
			return ReflectionUtil.newProxy(type, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			T target = this.target;
			if (target == null) {
				throw new ServiceUnavailableException("Proxy not bound to a type=" + type.getName() + ". "
															  + "Call AstrixRule.registerProvider to register a provider for the given bean");
			}
			return ReflectionUtil.invokeMethod(method, target, args);
		}
	}

	/**
	 * This service registry implementation intercepts all lookup attempts and ensures that
	 * there exists a proxy for the given service see {@link AstrixTestContext#registerProxy(Class)}
	 */
	private static class ProxiedServiceRegistry extends InMemoryServiceRegistry {
		public ProxiedServiceRegistry(MapConfigSource configSource) {
			super(configSource);
			set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
			set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false); // Explicit disable fault tolerance, might be overridden using Consumer<AstrixRuleContext> constructor
		}
	}
}

