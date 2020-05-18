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

import static com.gigaspaces.security.authorities.MonitorAuthority.MonitorPrivilege.MONITOR_JVM;
import static com.gigaspaces.security.authorities.MonitorAuthority.MonitorPrivilege.MONITOR_PU;

import java.util.Arrays;
import java.util.Objects;

import com.gigaspaces.security.Authority;
import com.gigaspaces.security.authorities.MonitorAuthority;
import com.gigaspaces.security.authorities.SpaceAuthority;
import com.gigaspaces.security.directory.UserDetails;
import com.google.common.base.MoreObjects;

public class GsUserDetails implements UserDetails {
	public static final String DEFAULT = "DEFAULT";
	private static final Authority[] EDIT_AUTHORITIES = new Authority[] {new SpaceAuthority(SpaceAuthority.SpacePrivilege.CREATE),
				new SpaceAuthority(SpaceAuthority.SpacePrivilege.TAKE), new SpaceAuthority(SpaceAuthority.SpacePrivilege.ALTER), new SpaceAuthority(SpaceAuthority.SpacePrivilege.WRITE),
				new SpaceAuthority(SpaceAuthority.SpacePrivilege.READ), new SpaceAuthority(SpaceAuthority.SpacePrivilege.EXECUTE)};
	private static final Authority[] MONITOR_AUTHORITIES = new Authority[] {new MonitorAuthority(MONITOR_JVM), new MonitorAuthority(MONITOR_PU)};
	private static final GsUserDetails MONITOR_USER = new GsUserDetails(DEFAULT, DEFAULT, MONITOR_AUTHORITIES);
	private final String username;
	private final String password;
	private final Authority[] authorities;

	private GsUserDetails(String username, String password, Authority[] authorities) {
		this.username = Objects.requireNonNull(username, "username");
		this.password = Objects.requireNonNull(password, "password");
		this.authorities = Objects.requireNonNull(authorities, "authorities");
	}

	public static GsUserDetails authenticated(String username, String password) {
		return new GsUserDetails(username, password, EDIT_AUTHORITIES);
	}

	public static GsUserDetails monitorUser() {
		return MONITOR_USER;
	}

	@Override
	public Authority[] getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public boolean matches(UserDetails details) {
		if (this == details) {
			return true;
		}
		if (details == null) {
			return false;
		}
		return Objects.equals(username, details.getUsername()) &&
				Objects.equals(password, details.getPassword());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GsUserDetails that = (GsUserDetails) o;
		return Objects.equals(username, that.username) &&
				Objects.equals(password, that.password) &&
				Arrays.equals(authorities, that.authorities);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(username, password);
		result = 31 * result + Arrays.hashCode(authorities);
		return result;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("username", username)
				.add("password", password)
				.add("authorities", authorities)
				.toString();
	}
}
