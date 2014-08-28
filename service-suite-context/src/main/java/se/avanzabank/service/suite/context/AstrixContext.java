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
package se.avanzabank.service.suite.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class AstrixContext implements Astrix {
	
	private final AstrixPlugins plugins;
	private final AstrixServiceRegistry serviceRegistry = new AstrixServiceRegistry();
	private List<ExternalDependencyBean> externalDependencies = new ArrayList<>();
	
	@Autowired
	public AstrixContext(AstrixPlugins plugins) {
		this.plugins = plugins;
	}
	
	public <T> T getPlugin(Class<T> type) {
		return plugins.getPlugin(type);
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	public void registerServiceProvider(AstrixServiceProvider serviceProvider) {
		this.serviceRegistry.registerProvider(serviceProvider);
	}

	public Astrix getAstrix() {
		// TODO: delete me
		return this;
	}

	/**
	 * Looks up a service in the local service registry. <p>
	 * 
	 * @param type
	 * @return
	 */
	public <T> T getService(Class<T> type) {
		return this.serviceRegistry.getService(type, this); // TODO: avoid passing 'this' reference by splitting AstrixContext into more abstractions if possible?
	}
	
	public <T> AstrixServiceFactory<T> getServiceFactory(Class<T> type) {
		return this.serviceRegistry.getServiceFactory(type);
	}
	
	public <T> AstrixServiceProvider getsServiceProvider(Class<T> type) {
		return this.serviceRegistry.getServiceProvider(type);
	}

	public AstrixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public <T> T waitForService(Class<T> class1, long timeoutMillis) {
		throw new UnsupportedOperationException();
	}

	public void setExternalDependencies(List<ExternalDependencyBean> externalDependencies) {
		this.externalDependencies = externalDependencies;
	}
	
	public <T extends ExternalDependencyBean> T getExternalDependency(Class<T> type) {
		for (ExternalDependencyBean dep : this.externalDependencies) {
			if (type.isAssignableFrom(dep.getClass())) {
				return type.cast(dep);
			}
		}
		throw new IllegalStateException("Missing dependency: " + type);
	}
	
}
