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


import static com.avanza.astrix.gs.security.GsUserDetails.DEFAULT;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.config.DynamicConfig;
import com.gigaspaces.security.AccessDeniedException;
import com.gigaspaces.security.Authentication;
import com.gigaspaces.security.AuthenticationException;
import com.gigaspaces.security.SecurityException;
import com.gigaspaces.security.SecurityManager;
import com.gigaspaces.security.directory.DirectoryManager;
import com.gigaspaces.security.directory.UserDetails;

public class GsSecurityManager implements SecurityManager {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private DynamicConfig dynamicConfig = null;

	public GsSecurityManager() {
		log.info("Instantiated security manager");
	}

	@Override
	public void init(Properties properties) throws SecurityException {
	}

	@Override
	public Authentication authenticate(UserDetails userDetails) throws AuthenticationException {
		GsUserDetails fromConfig = acquireUserDetails();
		// Matches on user and pw, gets edit privileges
		if(fromConfig.matches(userDetails)) {
			log.info("Successful authentication using username={}", userDetails.getUsername());
			return new Authentication(GsUserDetails.authenticated(userDetails.getUsername(), userDetails.getPassword()));
		} else {
			// Mismatch still gets to probe the space
			log.info("Unauthorized access attempt on username={}", userDetails.getUsername());
			return new Authentication(GsUserDetails.monitorUser());
		}
	}

	@Override
	public DirectoryManager createDirectoryManager(UserDetails userDetails) throws AuthenticationException, AccessDeniedException {
		log.warn("Tried to instantiate unimplemented directory manager");
		throw new AuthenticationException();
	}

	@Override
	public void close() {

	}

	private GsUserDetails acquireUserDetails() {
		if (dynamicConfig == null) {
			log.info("Trying to acquire config from context");
			dynamicConfig = ContextBridge.waitForConfig();
			if (dynamicConfig != null) {
				log.info("Successfully acquired config");
				return getFromConfig();
			} else {
				log.warn("Using monitor credentials");
				return GsUserDetails.monitorUser();
			}
		}
		return getFromConfig();
	}

	private GsUserDetails getFromConfig() {
			String user = dynamicConfig.getStringProperty("gs.user", DEFAULT).get();
			String pw = dynamicConfig.getStringProperty("gs.pw", DEFAULT).get();
			return GsUserDetails.authenticated(user, pw);
	}
}

