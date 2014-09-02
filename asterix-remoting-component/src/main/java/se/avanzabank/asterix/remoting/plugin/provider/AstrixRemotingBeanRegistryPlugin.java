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

import se.avanzabank.asterix.context.AstrixBeanRegistryPlugin;
import se.avanzabank.asterix.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.asterix.remoting.server.AstrixRemotingFrameworkBean;

/**
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixBeanRegistryPlugin.class)
public class AstrixRemotingBeanRegistryPlugin implements AstrixBeanRegistryPlugin {

	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		new AstrixRemotingFrameworkBean().postProcessBeanDefinitionRegistry(registry);
	}

	@Override
	public Class<? extends Annotation> getDescriptorType() {
		return AstrixRemoteApiDescriptor.class;
	}
	
}
