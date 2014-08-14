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

import java.util.Arrays;
import java.util.List;

public class AstrixContext {
	
	private AstrixObjectSerializerFactory objectSerializerFactory = AstrixObjectSerializerFactory.Default.noSerializationSupport();
	private AstrixFaultTolerance faultTolerance = AstrixFaultTolerance.Factory.noFaultTolerance();
	private List<AstrixServiceProviderPlugin<?>> serviceProviderPlugins = Arrays.<AstrixServiceProviderPlugin<?>>asList(new AstrixLibraryProviderPlugin());
	
	public List<AstrixServiceProviderPlugin<?>> getServiceProviderPlugins() {
		return serviceProviderPlugins;
	}
	
	public AstrixObjectSerializerFactory getObjectSerializerFactory() {
		return objectSerializerFactory;
	}
	
	public AstrixFaultTolerance getFaultTolerancePlugin() {
		return faultTolerance;
	}

	public void registerFalutTolerancePlugin(AstrixFaultTolerance faultTolerance) {
		this.faultTolerance = faultTolerance;
	}

	public void registerObjectSerializerPlugin(AstrixObjectSerializerFactory objectSerializerFactory) {
		this.objectSerializerFactory = objectSerializerFactory;
	}

	public void registerServiceProviderPlugins(List<AstrixServiceProviderPlugin<?>> serviceProviderPlugins) {
		this.serviceProviderPlugins = serviceProviderPlugins;
	}
	
	

}
