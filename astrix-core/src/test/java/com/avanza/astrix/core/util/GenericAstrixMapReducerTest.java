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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.test.util.AstrixTestUtil;


public class GenericAstrixMapReducerTest {

	@Test
	public void reduce_flattensMaps() throws Exception {
		GenericAstrixMapReducer<String, Integer> reducer = new GenericAstrixMapReducer<String, Integer>();
		assertEquals(newMap("a", 1).build(), reducer.reduce(Arrays.asList(new AstrixRemoteResult<>(newMap("a", 1).build(), null))));
		
		assertEquals(newMap("a", 1).with("b", 2).build(), reducer.reduce(Arrays.asList(
				new AstrixRemoteResult<>(newMap("a", 1).build(), null),
				new AstrixRemoteResult<>(newMap("b", 2).build(), null))));
		
		assertEquals(newMap("a", 1).with("b", 2).with("c", 3).build(), reducer.reduce(Arrays.asList(
				new AstrixRemoteResult<>(newMap("a", 1).with("c", 3).build(), null),
				new AstrixRemoteResult<>(newMap("b", 2).build(), null))));
	}
	
	@Test(expected = MyRuntimeException.class)
	public void reduce_rethrowsException() throws Exception {
		GenericAstrixMapReducer<String, Integer> reducer = new GenericAstrixMapReducer<String, Integer>();
		reducer.reduce(Arrays.asList(
				new AstrixRemoteResult<>(newMap("a", 1).build(), null),
				new AstrixRemoteResult<Map<String, Integer>>(null, new MyRuntimeException())));
	}
	
	public static AstrixTestUtil.MapBuilder newMap(String key, Integer value) {
		return new AstrixTestUtil.MapBuilder(key, value);
	}
	
	private static final class MyRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
}
