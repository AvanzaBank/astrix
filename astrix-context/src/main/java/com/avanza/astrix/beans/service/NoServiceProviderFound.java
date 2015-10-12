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
package com.avanza.astrix.beans.service;

import com.avanza.astrix.core.ServiceUnavailableException;

/**
 * Thrown when a service discovery attempt does not find a provider for the given service.
 * 
 * Some possible causes:
 * 
 * 1. The server has not yet registered in the service-registry.
 * 2. The server is miss-configured and does not "know" where the service registry is located.
 * 
 * @author Elias Lindholm
 *
 */
final class NoServiceProviderFound extends ServiceUnavailableException {
	private static final long serialVersionUID = 1L;
	public NoServiceProviderFound(String msg) {
		super(msg);
	}
}