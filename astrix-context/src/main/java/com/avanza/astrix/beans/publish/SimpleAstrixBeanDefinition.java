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

import java.util.Collections;
import java.util.Map;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public final class SimpleAstrixBeanDefinition<T> implements AstrixBeanDefinition<T> {

	private ApiProvider apiProvider;
	private AstrixBeanKey<T> beanKey;

	public SimpleAstrixBeanDefinition(ApiProvider apiProvider,
									  AstrixBeanKey<T> beanKey) {
		this.apiProvider = apiProvider;
		this.beanKey = beanKey;
	}

	@Override
	public AstrixBeanKey<T> getBeanKey() {
		return this.beanKey;
	}

	@Override
	public ApiProvider getDefiningApi() {
		return this.apiProvider;
	}

}
