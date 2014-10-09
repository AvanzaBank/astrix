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
package se.avanzabank.asterix.remoting.server;

import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;
/**
 * This bean makes all beans annotated with AsterixRemoteServiceExport invokable using the remoting 
 * framework. 
 * 
 * Note that this bean does not publish any services onto the service registry.  
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixRemotingServiceExporterBean implements AsterixServiceExporterBean {
	
	private AsterixServiceActivator serviceActivator;

	@Autowired
	public AsterixRemotingServiceExporterBean(AsterixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}

	@Override
	public void register(Object provider, AsterixApiDescriptor apiDescriptor, Class<?> providedApi) {
		this.serviceActivator.register(provider, apiDescriptor, providedApi);
	}
	
	@Override
	public String getTransport() {
		return AsterixServiceComponentNames.GS_REMOTING;
	}

}
