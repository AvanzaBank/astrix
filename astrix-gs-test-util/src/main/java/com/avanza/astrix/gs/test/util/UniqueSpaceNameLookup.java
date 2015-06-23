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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

final class UniqueSpaceNameLookup {
	
	private static final ConcurrentMap<String, AtomicInteger> countBySpaceName = new ConcurrentHashMap<>();
	
	public static String getSpaceNameWithSequence(String spaceName) {
		AtomicInteger counter = countBySpaceName.get(spaceName);
		if (counter == null) {
			counter = new AtomicInteger(0);
			countBySpaceName.putIfAbsent(spaceName, counter);
			counter = countBySpaceName.get(spaceName);
		}
		return spaceName + "-" + counter.incrementAndGet();
	}

}
