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
package com.avanza.astrix.beans.registry;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.publish.AstrixConfigAware;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;

@AstrixApiProvider
public class AstrixServiceRegistryLibraryProvider implements AstrixConfigAware {

	private DynamicConfig config;

	@Library
	public ServiceRegistryClient createClient(AstrixServiceRegistry serviceRegistry) {
		ServiceConsumerProperties serviceConsumerProperties = new ServiceConsumerProperties();
		String subsystem = AstrixSettings.SUBSYSTEM_NAME.getFrom(config).get();
		String applicationTag = AstrixSettings.APPLICATION_TAG.getFrom(config).get();
		String zone = subsystem;
		if (applicationTag != null) {
			zone = subsystem + "#" + applicationTag;
		}
		serviceConsumerProperties.setProperty(ServiceConsumerProperties.CONSUMER_ZONE, zone);
		return new ServiceRegistryClient(serviceRegistry, serviceConsumerProperties);
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
}
