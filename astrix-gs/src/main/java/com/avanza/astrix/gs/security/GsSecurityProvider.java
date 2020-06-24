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
package com.avanza.astrix.gs.security;

import com.gigaspaces.security.Authentication;
import com.gigaspaces.security.directory.CredentialsProvider;
import com.gigaspaces.security.directory.UserDetails;

public interface GsSecurityProvider {
	/**
	 * Provides client credentials when making connections to a secured
	 * GigaSpaces space. This method will be invoked when a new connection
	 * is being established to a remote space. If the space does not require
	 * authentication, this method will not be called.
	 *
	 * @param spaceName the name of the space that a client intends to
	 *                  connect to
	 * @return {@link CredentialsProvider}
	 */
	CredentialsProvider getGsClientCredentialsProvider(String spaceName);

	/**
	 * Provides a server-side authentication validator for credentials that the
	 * server has received. This method will be invoked when the GigaSpaces
	 * component is starting up (when the service is starting), regardless of
	 * whether security is enabled or not. However, the methods on
	 * {@link GsServerAuthenticator} will only be invoked for secured spaces
	 * when the server has received client credentials.
	 *
	 * @return {@link GsServerAuthenticator}
	 */
	GsServerAuthenticator getGsServerAuthenticator();

	interface GsServerAuthenticator {
		Authentication authenticate(UserDetails userDetails);
	}
}
