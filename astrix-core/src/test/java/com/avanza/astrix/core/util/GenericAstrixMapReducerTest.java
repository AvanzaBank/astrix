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

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.ServiceInvocationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class GenericAstrixMapReducerTest {

	@Test
	void reduce_flattensMaps() {
		GenericAstrixMapReducer<String, Integer> reducer = new GenericAstrixMapReducer<>();
		assertEquals(newMap("a", 1).build(), reducer.reduce(singletonList(AstrixRemoteResult.successful(newMap("a", 1).build()))));
		
		assertEquals(newMap("a", 1).with("b", 2).build(), reducer.reduce(Arrays.asList(
				AstrixRemoteResult.successful(newMap("a", 1).build()),
				AstrixRemoteResult.successful(newMap("b", 2).build()))));
		
		assertEquals(newMap("a", 1).with("b", 2).with("c", 3).build(), reducer.reduce(Arrays.asList(
				AstrixRemoteResult.successful(newMap("a", 1).with("c", 3).build()),
				AstrixRemoteResult.successful(newMap("b", 2).build()))));
	}
	
	@Test
	void reduce_rethrowsException() {
		GenericAstrixMapReducer<String, Integer> reducer = new GenericAstrixMapReducer<>();
		assertThrows(MyRuntimeException.class, () ->reducer.reduce(Arrays.asList(
				AstrixRemoteResult.successful(newMap("a", 1).build()),
				AstrixRemoteResult.failure(new MyRuntimeException(), CorrelationId.undefined()))));
	}
	
	static MapBuilder newMap(String key, Integer value) {
		return new MapBuilder(key, value);
	}
	
	private static final class MyRuntimeException extends ServiceInvocationException {
		private static final long serialVersionUID = 1L;
		public MyRuntimeException() {
			super();
		}
		@Override
		public ServiceInvocationException recreateOnClientSide() {
			return new MyRuntimeException();
		}
	}
	
	public static class MapBuilder {
		private final Map<String, Integer> result = new HashMap<>();
		public MapBuilder() { }
		public MapBuilder(String key, Integer value) { 
			with(key, value);
		}
		public MapBuilder with(String key, Integer value) {
			result.put(key, value);
			return this;
		}
		public Map<String, Integer> build() {
			return result;
		}
	}
}
