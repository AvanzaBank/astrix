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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceFrameworkBean implements  BeanDefinitionRegistryPostProcessor {
	
	// TODO: rename to AstrixRemotingFrameworkBean???
	

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixContext.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_astrixContext", beanDefinition);
		
		beanDefinition = lookupAstrixComponent(AstrixVersioningPlugin.class);
		registry.registerBeanDefinition("_astrixVersioningPlugin", beanDefinition);
		
		beanDefinition = lookupAstrixComponent(AstrixFaultTolerancePlugin.class);
		registry.registerBeanDefinition("_astrixVersioningPlugin", beanDefinition);
		
//		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceExporterBean.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_astrixServiceExporterBean", beanDefinition);
//		
//		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixRemoteServiceProviderFactory.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_astrixRemoteServiceProviderFactory", beanDefinition);
//		
//		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixRemoteServiceProvider.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		beanDefinition.setFactoryBeanName("_astrixRemoteServiceProviderFactory");
//		beanDefinition.setFactoryMethodName("create");
//		registry.registerBeanDefinition("_astrixRemoteServiceProvider", beanDefinition);
//		
////		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixRemotingArgumentSerializerFactory.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_astrixRemotingArgumentSerializerFactory", beanDefinition);
//		
//		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixObjectSerializer.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		beanDefinition.setScope(AbstractBeanDefinition.SCOPE_SINGLETON);
//		beanDefinition.setFactoryBeanName("_astrixRemotingArgumentSerializerFactory");
//		beanDefinition.setFactoryMethodName("create");
//		registry.registerBeanDefinition("_astrixRemotingArgumentSerializer", beanDefinition);
	}

	private AnnotatedGenericBeanDefinition lookupAstrixComponent(Class<?> plugin) {
		AnnotatedGenericBeanDefinition beanDefinition;
		beanDefinition = new AnnotatedGenericBeanDefinition(plugin);
		beanDefinition.setFactoryBeanName("_astrixContext");
		beanDefinition.setFactoryMethodName("create");
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, plugin);
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		return beanDefinition;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// intentionally empty, inherited from BeanDefinitionRegistryPostProcessor
	}
}