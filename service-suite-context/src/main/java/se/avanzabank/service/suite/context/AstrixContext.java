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

import java.util.List;

public class AstrixContext {
	
	private final AstrixPlugins plugins = new AstrixPlugins();
	private AstrixImpl astrix = new AstrixImpl();
	
	public AstrixContext() {
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
		this.astrix.registerServiceProvider(serviceProvider);
	}

	public Astrix getAstrix() {
		return this.astrix;
	}

	/**
	 * Looks up a service in the local service registry. <p>
	 * 
	 * @param type
	 * @return
	 */
	public <T> T getService(Class<T> type) {
		return this.astrix.getService(type);
	}
	
	
}
