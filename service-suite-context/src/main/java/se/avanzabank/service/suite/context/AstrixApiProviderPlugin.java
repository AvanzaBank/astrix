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

import java.lang.annotation.Annotation;
import java.util.List;


/**
 * An AstrixApiProviderPlugin is responsible for creating AstrixFactoryBean's for
 * all parts of a given "type" of api. By "type" in this context, we don't mean the
 * different api's that are hooked into astrix for consumption, but rather a mechanism
 * for an api-provider to use to make the different part's of the api available for consumption.
 * 
 * For instance, one type of api is "library", which is handled by the AstrixLibraryProviderPlugin. It
 * allows api-providers to export api's that require "a lot" of wiring on the "client side" by annotation
 * a class with @AstrixLibraryProvider an export different api-elements by annotating factory methods
 * for different api elements with @AstrixExport. 
 * 
 * Another type of api is a pure "astrix-remoting" api, which also requires a server side component to respond
 * to the incoming service-request.
 * 
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixApiProviderPlugin {
	
	List<AstrixFactoryBean<?>> createFactoryBeans(Class<?> apiDescriptorHolder);
	
	@Deprecated // TODO: remove, replaced by consumes
	Class<? extends Annotation> getProviderAnnotationType();
	
	boolean consumes(Class<?> descriptorHolder);
	
}