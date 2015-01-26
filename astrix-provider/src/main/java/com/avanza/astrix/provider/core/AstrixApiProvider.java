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
package com.avanza.astrix.provider.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Used to identify an ApiProvider for a given api. 
 * 
 * An ApiProvider is used to define all Astrix-beans that is part of a given api, and
 * to provide information about how to bind to an instance of a each bean in the api at runtime. 
 * 
 * Astrix defines two distinct types of beans: "libraries" and "services". Simply put, a "service"
 * can be seen as a bean that uses some kind of remote-service-invocation mechanism to invoke a provider of
 * a given bean-type, whereas a "library" uses simple in-memory method calls.
 * 
 * A more formal distinction between a "library" and a "service" is how they are bound at runtime. 
 * For libraries, the class holding this annotation should define a factory method for each exported library bean in
 * the api. At runtime Astrix binds to a provider of a given bean type by invoking the corresponding factory 
 * method and use the returned instance as provider. Such a method should be annotated with @Library indicating:
 * 
 * <ol>
 * <li> That this api provides a library bean of the type returned by the method
 * <li> That the method should be used as a factory to bind to a provider of the given bean type
 * </ol>
 * 
 * The other type of bean is "service". For services Astrix uses an AstrixServiceComponent to bind to a provider
 * of the given bean type. The AstrixServiceComponent is an extension point to create custom service transports.
 * A service is defined by annotating a method with @Service indicating:
 * 
 * <ol>
 * <li> That this api provides a service of the type returned by the method
 * <li> Optional information about how to "lookup" the given service, by default Astrix will
 * lookup a given service using the service-registry.
 * </ol>
 * 
 * At runtime, binding of a service is done as follows:
 * 
 * <ol>
 * <li> Use the defined lookup mechanism to retrieve runtime service information. The information retrieved
 * from lookup contains information about (a.) what AstrixServiceComponent to use to bind to the provider and
 * (b.) all service properties required by the AstrixServiceComponent to bind to a provider.
 * <li> Use the service-properties and AstrixServiceComponent retrieved from lookup to bind to a provider.
 * </ol>
 * 
 * Libraries are sometimes referred to as "statically bound Astrix beans" reflecting that 
 * the Astrix bean instance can't change at runtime. Accordingly, services are sometimes referred to
 * as "dynamically bound Astrix beans", reflecting that they can be re-bind to a new provider at runtime. 
 * For instance, if a new provider for a given service is registered in the service registry, then
 * Astrix transparently re-binds the corresponding astrix-bean to the new provider. The new provider
 * might be using another transport mechanism (as defined by the corresponding AstrixServiceComponent), allowing
 * transport mechanism to change at runtime. 
 * 
 * Example of a service provider exporting two services that are located using the service registry:
 * 
 * <pre>
 * @AstrixApiProvider
 * interface MyServiceProvider {
 *     @Service
 *     MyService service();
 *     
 *     @Service
 *     AnotherService anotherService();
 * }
 * </pre>
 * 
 * 
 * Example library provider exporting a single library Astrix bean. 
 * 
 * <pre>
 * @AstrixApiProvider
 * class MyLibProvider {
 *     @Library
 *     MyLibrary lib() {
 *         return new MyLibImpl();
 *     }
 * }
 * </pre>
 * 
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
@Target(value = { ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixApiProvider {

	/**
	 * @return
	 * @deprecated Don't split api into "api-definition" class and "api-provider" class anymore.
	 * 
	 * For instance, don't do this:
	 * 
	 * class MyApi {
	 *     @Service
	 *     MySevice service();
	 * }
	 * 
	 * @AstrixApiProvider(MyApi.class)
	 * class MyApiProvider {
	 * 
	 * }
	 * 
	 * Put the AstrixApiProvider annotation directly on the same class/interface that defines the api.
	 * 
	 * For instance:
	 * 
	 * @AstrixApiProvider
	 * interface MyServiceProvider {
	 *     @Service
	 *     MyService service();
	 *     
	 *     // Other service definition
	 * }
	 * 
	 * Or for a library:
	 * 
	 * @AstrixApiProvider
	 * class MyLibProvider {
	 *     @Library
	 *     MyLibrary lib() {
	 *         return new MyLibImpl();
	 *     }
	 *     
	 *     // Other library definitions
	 * }
	 * 
	 * 
	 * 
	 */
	@Deprecated
	Class<?>[] value() default {};
	
}
