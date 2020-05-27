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

import java.util.Objects;

import com.avanza.astrix.config.DynamicConfig;
import com.gigaspaces.security.directory.CredentialsProvider;
import com.gigaspaces.security.directory.UserDetails;

/**
 *
 * Does not actually need to be externalizable, can be provided from spring context
 */
public class GsCredentialsProvider extends CredentialsProvider {
	private final DynamicConfig dynamicConfig;

	public GsCredentialsProvider(DynamicConfig dynamicConfig) {
		this.dynamicConfig = Objects.requireNonNull(dynamicConfig, "dynamicConfig");
	}

	@Override
	public UserDetails getUserDetails() {
		return GsUserDetails.authenticated(dynamicConfig.getStringProperty("gs.user", DEFAULT).get(),
				dynamicConfig.getStringProperty("gs.pw", DEFAULT).get());
	}
}
