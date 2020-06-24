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

import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.avanza.astrix.gs.security.GsSecurityProvider.GsServerAuthenticator;
import com.gigaspaces.security.AccessDeniedException;
import com.gigaspaces.security.Authentication;
import com.gigaspaces.security.AuthenticationException;
import com.gigaspaces.security.SecurityException;
import com.gigaspaces.security.SecurityManager;
import com.gigaspaces.security.directory.DirectoryManager;
import com.gigaspaces.security.directory.UserDetails;

public class GsSecurityManager implements SecurityManager {
	private static final Logger LOG = LoggerFactory.getLogger(GsSecurityManager.class);
	private static GsServerAuthenticator authenticator;

	/**
	 * Default constructor without arguments is REQUIRED by GigaSpaces, since
	 * it is created using reflection from
	 * {@link com.gigaspaces.security.SecurityFactory#createSecurityManager}
	 */
	public GsSecurityManager() {
		LOG.info("Created security manager");
	}

	public static void setGsServerAuthenticator(GsServerAuthenticator authenticator) {
		GsSecurityManager.authenticator = Objects.requireNonNull(authenticator);
		LOG.info("GsSecurityManager has been initialized with " + authenticator.getClass().getName());
	}

	@Override
	public void init(Properties properties) throws SecurityException {
	}

	@Override
	public Authentication authenticate(UserDetails userDetails) throws AuthenticationException {
		if (authenticator == null) {
			throw new AuthenticationException("GsSecurityManager is not initialized");
		}
		LOG.info("Authenticating user={}", userDetails.getUsername());
		return authenticator.authenticate(userDetails);
	}

	@Override
	public DirectoryManager createDirectoryManager(UserDetails userDetails) throws AuthenticationException, AccessDeniedException {
		throw new UnsupportedOperationException("DirectoryManager not supported by GsSecurityManager");
	}

	@Override
	public void close() {
	}
}

