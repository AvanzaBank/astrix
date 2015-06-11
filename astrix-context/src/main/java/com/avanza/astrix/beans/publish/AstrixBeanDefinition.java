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
package com.avanza.astrix.beans.publish;

import com.avanza.astrix.beans.core.AstrixBeanKey;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface AstrixBeanDefinition<T> {
	
	// TODO: Is BeanDefinition the correct name for this class? Isn't it more like a PublishedBeanDefinition? or just an PublishedAstrixBean?
	
	AstrixBeanKey<T> getBeanKey();

	ApiProvider getDefiningApi();
	
}
