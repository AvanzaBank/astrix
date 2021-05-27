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

import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.Setting;
import com.avanza.astrix.context.Astrix;
import com.avanza.astrix.context.AstrixContext;

import java.util.function.Consumer;

abstract class CommonAstrixTestSupport {

	private final AstrixTestContext astrixTestContext;

	@SafeVarargs
	protected CommonAstrixTestSupport(Class<? extends TestApi>... testApis) {
		this.astrixTestContext = new AstrixTestContext(testApis);
	}

	@SafeVarargs
	protected CommonAstrixTestSupport(Consumer<? super AstrixRuleContext> contextConfigurer, Class<? extends TestApi>... testApis) {
		this(testApis);
		contextConfigurer.accept(new AstrixRuleContext() {
            @Override
            public <T> void registerProxy(Class<T> service) {
            	astrixTestContext.registerProxy(service);
            }

            @Override
            public <T> void registerProxy(Class<T> service, String qualifier) {
            	astrixTestContext.registerProxy(service, qualifier);
            }

            @Override
			public <T> void set(Setting<T> setting, T value) {
				astrixTestContext.set(setting, value);
			}
		});

	}

	/**
	 * The configSourceId might be used to retrieve the ConfigSource instance
	 * using {@link GlobalConfigSourceRegistry#getConfigSource(String)}
	 *
	 * @return the configSourceId for the associated ConfigSource.
	 */
	public String getConfigSourceId() {
		return astrixTestContext.getConfigSourceId();
	}

	/**
	 * @return the serviceUri for the associated service-registry.
	 */
	public String getServiceRegistryUri() {
		return astrixTestContext.getServiceRegistryUri();
	}

	public void destroy() {
		this.astrixTestContext.destroy();
	}

	/**
	 * @see AstrixRuleContext#registerProxy(Class)
	 *
	 * @param service - The qualified bean type to register a proxy for
	 */
    public <T> void registerProxy(Class<T> service) {
    	astrixTestContext.registerProxy(service);
    }

	/**
	 * @see AstrixRuleContext#registerProxy(Class)
	 *
	 * @param service - The qualified bean type to register a proxy for
	 * @param qualifier - The qualifier of the bean type to register a proxy for
	 */
    public <T> void registerProxy(Class<T> service, String qualifier) {
        astrixTestContext.registerProxy(service, qualifier);
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
		astrixTestContext.setProxyState(type, mock);
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
		astrixTestContext.setProxyState(type, qualifier, mock);
    }

	public void resetProxies() {
		astrixTestContext.resetProxies();
	}

	public <T> T waitForBean(Class<T> type, long timeoutMillis) throws InterruptedException {
		return astrixTestContext.waitForBean(type, timeoutMillis);
	}

	public <T> T waitForBean(Class<T> type, String qualifier, long timeoutMillis) throws InterruptedException {
		return astrixTestContext.waitForBean(type, qualifier, timeoutMillis);
	}

	public Astrix getAstrix() {
		return astrixTestContext.getAstrixContext();
	}

	public AstrixContext getAstrixContext() {
		return astrixTestContext.getAstrixContext();
	}

	public <T extends TestApi> T getTestApi(Class<T> testApi) {
		return astrixTestContext.getTestApi(testApi);
	}

	public void resetTestApis() {
		this.astrixTestContext.resetTestApis();
	}

	<T> T getBean(Class<T> serviceBean) {
		return astrixTestContext.getBean(serviceBean);
	}
	
	<T> T getBean(Class<T> serviceBean, String qualifier) {
		return astrixTestContext.getBean(serviceBean, qualifier);
	}

	public void setConfigurationProperty(String settingName, String value) {
		astrixTestContext.setConfigurationProperty(settingName, value);
	}

}

