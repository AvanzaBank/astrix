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
package se.avanzabank.asterix.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
/**
 * Registers beans associated with consumption of asterix beans. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixClientRuntimeBuilder {

	private List<Class<?>> consumedAsterixBeans;
	private AsterixContext asterixContext;
	private Map<String, String> settings;
	
	public AsterixClientRuntimeBuilder(AsterixContext asterixContext, Map<String, String> settings, List<Class<?>> consumedAsterixBeans) {
		this.asterixContext = asterixContext;
		this.settings = settings;
		this.consumedAsterixBeans = new ArrayList<>(consumedAsterixBeans);
	}

	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		MutablePropertyValues asterixConfigurerProps = new MutablePropertyValues();
//		asterixConfigurerProps.add("settings", this.settings);
		asterixConfigurerProps.add("subsystem", this.asterixContext.getCurrentSubsystem());
		beanDefinition.setPropertyValues(asterixConfigurerProps);
		registry.registerBeanDefinition("_asterixConfigurer", beanDefinition);

		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixContext.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_asterixConfigurer");
		beanDefinition.setFactoryMethodName("configure");
		registry.registerBeanDefinition("_asterixContext", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_asterixContext");
		beanDefinition.setFactoryMethodName("getPlugins");
		registry.registerBeanDefinition("_asterixPlugins", beanDefinition);
		
		
		for (Class<?> consumedAsterixBean : consumedAsterixBeans) {
			beanDefinition = new AnnotatedGenericBeanDefinition(AsterixSpringFactoryBean.class);
			MutablePropertyValues props = new MutablePropertyValues();
			props.add("type", consumedAsterixBean);
			beanDefinition.setPropertyValues(props);
			registry.registerBeanDefinition("_" + consumedAsterixBean.getName(), beanDefinition);
		}
	}

}
