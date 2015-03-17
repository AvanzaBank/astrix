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
package com.avanza.astrix.provider.component;
/**
 * Contains well-known AstrixServiceComponent implementations. <p>
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceComponentNames {
	
	/**
	 * The direct component allows services to bind to provider instances within the same
	 * jvm. Its mainly intended for testing purposes.
	 */
	public static final String DIRECT = "direct";
	
	/**
	 * This component can only be used to export GigaSpace as a service. Any
	 * attempt to export other service-interfaces using this component will throw
	 * UnsupportedOperationException.
	 */
	public static final String GS = "gs";
	
	/**
	 * This component can be used to export remote services using gigaspaces
	 * as service transport.
	 */
	public static final String GS_REMOTING = "gs-remoting";
	
	/**
	 * Used export local-view GigaSpace as a service. Requires a LocalViewConfigurer
	 * class to be defined as additional service configuration, see @ServiceConfig
	 */
	public static final String GS_LOCAL_VIEW = "gs-local-view";

}
