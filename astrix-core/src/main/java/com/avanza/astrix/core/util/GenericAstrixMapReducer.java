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
package com.avanza.astrix.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRemoteResultReducer;

/**
 * Reduce {@link Map}s into one {@link Map} containing the union of all input data
 * @author joasah
 */
public class GenericAstrixMapReducer<K, V> implements AstrixRemoteResultReducer<Map<K, V>, Map<K, V>> {

	@Override
	public Map<K, V> reduce(List<AstrixRemoteResult<Map<K, V>>> results) {
		Map<K, V> map = new HashMap<K, V>();
		for (AstrixRemoteResult<Map<K, V>> result : results) {
			map.putAll(result.getResult());
		}
		return map;
	}

}