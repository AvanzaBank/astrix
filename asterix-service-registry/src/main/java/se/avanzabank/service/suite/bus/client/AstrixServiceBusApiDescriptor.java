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
package se.avanzabank.service.suite.bus.client;

import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.service.suite.provider.versioning.AstrixVersioned;

/**
 * The serivce-bus-api uses astrix-remoting to export its service. Note that
 * it doesn't use the service-bus to bind to the providers, but rather uses a
 * SpaceLocator and the space-name provided here. 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@AstrixRemoteApiDescriptor (
	exportedApis = {
		AstrixServiceBus.class,
	},
	targetSpaceName = "service-bus-space"
)
public class AstrixServiceBusApiDescriptor {
}


