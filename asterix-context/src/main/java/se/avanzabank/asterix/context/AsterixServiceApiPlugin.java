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

import java.lang.annotation.Annotation;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceApiPlugin {
	
	// This is a server-side only component. Change name to reflect that
	
//	void registerBeanDefinitions(BeanDefinitionRegistry registry, AsterixServiceDescriptor descriptor) throws BeansException;
	
//	void registerBeanDefinitions(BeanDefinitionRegistry registry);
	
	// TODO: move these methods to asterixservicetransport
	
	Class<? extends Annotation> getServiceDescriptorType();
	
	String getTransport();

}
