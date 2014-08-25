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

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
/**
 * 
 * 
 * Responsible for registering all services consumed by the given application in the
 * application context. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixBeanRegistryPlugin.class)
public class AstrixServiceConsumerBeanRegistryPlugin implements AstrixBeanRegistryPlugin {
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_astrixPlugins", beanDefinition);
		
//		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixContext.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_astrixContext", beanDefinition);
//		
//		beanDefinition = lookupAstrixComponent(AstrixVersioningPlugin.class);
//		registry.registerBeanDefinition("_astrixVersioningPlugin", beanDefinition);
//		
//		beanDefinition = lookupAstrixComponent(AstrixFaultTolerancePlugin.class);
//		registry.registerBeanDefinition("_astrixFaultTolerancePlugin", beanDefinition);
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

}
