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

import com.avanza.astrix.config.Setting;
import com.avanza.astrix.core.ServiceUnavailableException;

public interface AstrixRuleContext {

	<T> void set(Setting<T> setting, T value);

	/**
	 * Registers a proxy for a given service in the service registry.
	 * 
	 * The benefit of registering a proxy is that all consumers of the
	 * service will be able to bind to the proxy on the first service discovery attempt. 
	 * The state of the proxy can later be changed by calling {@link AstrixRule#setProxyState(Class, Object)}, 
	 * after  which all consumers immediately sees the new state of the service. 
	 * No waiting for new service-discovery attempts by the consumers of the service i necessary.
	 * 
	 * In the initial state the proxy always throws {@link ServiceUnavailableException} on
	 * each invocation, i.e it behaves in the same way as if there
	 * where no provider available for the given service. 
	 * 
	 * @param service - The (unqualified) bean type to register a proxy for
	 */
	<T> void registerProxy(Class<T> service);	

	
	/**
	 * Registers a proxy for a given service in the service registry.
	 * 
	 * The benefit of registering a proxy is that all consumers of the
	 * service will be able to bind to the proxy on the first service discovery attempt. 
	 * The state of the proxy can later be changed by calling {@link AstrixRule#setProxyState(Class, Object)}, 
	 * after  which all consumers immediately sees the new state of the service. 
	 * No waiting for new service-discovery attempts by the consumers of the service i necessary.
	 * 
	 * In the initial state the proxy always throws {@link ServiceUnavailableException} on
	 * each invocation, i.e it behaves in the same way as if there
	 * where no provider available for the given service. 
	 * 
	 * @param service - The qualified bean type to register a proxy for
	 * @param qualifier - The qualifier of the bean type to register a proxy for
	 */
	<T> void registerProxy(Class<T> service, String qualifier);
	

}
