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
package se.avanzabank.asterix.remoting.plugin.provider;

import java.lang.annotation.Annotation;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixServiceApiPlugin;
import se.avanzabank.asterix.context.AsterixServiceDescriptor;
import se.avanzabank.asterix.provider.component.AsterixServiceRegistryComponentNames;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;
import se.avanzabank.asterix.remoting.server.AsterixRemotingFrameworkBean;

/**
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AsterixServiceApiPlugin.class)
public class AsterixRemotingBeanRegistryPlugin implements AsterixServiceApiPlugin {

//	@Override
//	public void registerBeanDefinitions(BeanDefinitionRegistry registry, AsterixServiceDescriptor descriptor) throws BeansException {
//		new AsterixRemotingFrameworkBean().postProcessBeanDefinitionRegistry(registry);
//	}
	
	@Override
	public Class<? extends Annotation> getServiceDescriptorType() {
		return AsterixRemoteApiDescriptor.class;
	}

	@Override
	public String getTransport() {
		return AsterixServiceRegistryComponentNames.GS_REMOTING;
	}
	
//	@Override
//	public void registerBeanDefinitions(BeanDefinitionRegistry registry) {
//		// Intentionally empty
//	}
	
	
}
