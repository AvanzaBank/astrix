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
package com.avanza.astrix.gs.test.util;

import java.util.UUID;

import org.openspaces.core.space.UrlSpaceConfigurer;

import com.j_spaces.core.IJSpace;

public final class JVMGlobalLus {
	
	/*
	 * GigaSpaces 10.1 only allows running a single instance of the lus per jvm.
	 * 
	 * The lus is shutdown asynchronously when terminating a processing unit. This
	 * seems to cause problems between test runs that starts an integrated
	 * processing unit.
	 *  
	 */

	private static final String LOOKUP_GROUP_NAME = UUID.randomUUID().toString();
	private static final IJSpace DUMMY_SPACE;
	
	static {
		DUMMY_SPACE = new UrlSpaceConfigurer("/./dummy-space-to-keep-lus-running").lookupGroups(LOOKUP_GROUP_NAME).create();
	}
	
	public static String getLookupGroupName() {
		return LOOKUP_GROUP_NAME;
	}
	
	

}
