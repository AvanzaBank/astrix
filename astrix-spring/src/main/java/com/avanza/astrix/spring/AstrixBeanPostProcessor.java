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
package com.avanza.astrix.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.serviceunit.ServiceExporter;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixBeanPostProcessor implements BeanPostProcessor {
	
	private ServiceExporter serviceExporter;
	
	public AstrixBeanPostProcessor(ServiceExporter serviceExporter) {
		this.serviceExporter = serviceExporter;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean.getClass().isAnnotationPresent(AstrixServiceExport.class)) {
			serviceExporter.addServiceProvider(bean);
		}
		return bean;
	}


	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
	
}
