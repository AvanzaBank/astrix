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

/**
 * Bean is the generic name used for each api element that astrix manages. It
 * might be in the form of a service 'remoting service', in the form of a library.
 * 
 * Bean is the generic term used for an instance of a given type. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface AstrixFactoryBean<T> {
	
	T create();
	
	Class<T> getBeanType();
	
}
