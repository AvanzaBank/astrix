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

import java.lang.annotation.Annotation;
import java.util.List;

import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.provider.core.AstrixApiProvider;


/**
 * An ApiProviderPlugin is responsible for creating a {@link FactoryBean} for
 * all parts of a given "type" of api. By "type" in this context, we don't mean the
 * different api's that are hooked into Astrix for consumption, but rather a mechanism
 * for an api-provider to publish the different part's of the api, thereby allowing it
 * to be consumed using Astrix.<p>
 * 
 * Astrix comes with the {@link GenericAstrixApiProviderPlugin} which handles api-providers
 * annotated with {@link AstrixApiProvider}, see {@link AstrixApiProvider} for more information.<p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface ApiProviderPlugin {
	
	List<PublishedBean> createFactoryBeans(ApiProviderClass apiProviderClass);
	
	Class<? extends Annotation> getProviderAnnotationType();
	
}