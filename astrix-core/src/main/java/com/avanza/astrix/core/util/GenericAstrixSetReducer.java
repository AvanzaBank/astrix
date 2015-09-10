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
package com.avanza.astrix.core.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.RemoteResultReducer;

/**
 * Reduce {@link Set}s into one {@link Set} containing the union of all unique input data.
 * 
 */
public class GenericAstrixSetReducer<T> implements RemoteResultReducer<Set<T>> {

	@Override
	public Set<T> reduce(List<AstrixRemoteResult<Set<T>>> results) {
		Set<T> set = new HashSet<>();
		for (AstrixRemoteResult<Set<T>> result : results) {
			set.addAll(result.getResult());
		}
		return set;
	}

}