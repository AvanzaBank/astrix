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
package com.avanza.astrix.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
/**
 * Registers beans associated with consumption of Astrix beans. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixClientRuntimeBuilder {

	private List<Class<?>> consumedAstrixBeans;
	private AstrixContext AstrixContext;
	private Map<String, String> settings;
	
	public AstrixClientRuntimeBuilder(AstrixContext AstrixContext, Map<String, String> settings, List<Class<?>> consumedAstrixBeans) {
		this.AstrixContext = AstrixContext;
		this.settings = settings;
		this.consumedAstrixBeans = new ArrayList<>(consumedAstrixBeans);
	}

	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		MutablePropertyValues AstrixConfigurerProps = new MutablePropertyValues();
//		AstrixConfigurerProps.add("settings", this.settings);
		AstrixConfigurerProps.add("subsystem", this.AstrixContext.getCurrentSubsystem());
		beanDefinition.setPropertyValues(AstrixConfigurerProps);
		registry.registerBeanDefinition("_AstrixConfigurer", beanDefinition);

		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixContext.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_AstrixConfigurer");
		beanDefinition.setFactoryMethodName("configure");
		registry.registerBeanDefinition("_AstrixContext", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_AstrixContext");
		beanDefinition.setFactoryMethodName("getPlugins");
		registry.registerBeanDefinition("_AstrixPlugins", beanDefinition);
		
		
		for (Class<?> consumedAstrixBean : consumedAstrixBeans) {
			beanDefinition = new AnnotatedGenericBeanDefinition(AstrixSpringFactoryBean.class);
			MutablePropertyValues props = new MutablePropertyValues();
			props.add("type", consumedAstrixBean);
			beanDefinition.setPropertyValues(props);
			registry.registerBeanDefinition("_" + consumedAstrixBean.getName(), beanDefinition);
		}
	}

}
