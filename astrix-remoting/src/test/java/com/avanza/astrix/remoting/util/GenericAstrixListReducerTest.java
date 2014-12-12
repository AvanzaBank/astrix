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
package com.avanza.astrix.remoting.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.avanza.astrix.core.AstrixRemoteResult;


public class GenericAstrixListReducerTest {

	@Test
	public void reduce_flattensIncomingLists() throws Exception {
		GenericAstrixListReducer<String> genericAstrixListReducer = new GenericAstrixListReducer<>();
		assertEquals(Arrays.asList("hej"), genericAstrixListReducer.reduce(asList(
				new AstrixRemoteResult<>(asList("hej"), null))));
		assertEquals(Arrays.asList("hej", "då"), genericAstrixListReducer.reduce(asList(
				new AstrixRemoteResult<>(asList("hej"), null),
				new AstrixRemoteResult<>(asList("då"), null))));
		assertEquals(Arrays.asList("hej", "då", "re"), genericAstrixListReducer.reduce(asList(
				new AstrixRemoteResult<>(asList("hej", "då"), null),
				new AstrixRemoteResult<>(asList("re"), null))));
	}
	
	@Test(expected = MyRuntimeException.class)
	public void reduce_rethrowsExceptions() throws Exception {
		GenericAstrixListReducer<String> genericAstrixListReducer = new GenericAstrixListReducer<>();
		genericAstrixListReducer.reduce(asList(
				new AstrixRemoteResult<>(asList("hej"), null),
				new AstrixRemoteResult<List<String>>(null, new MyRuntimeException())));
	}
	
	private static final class MyRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

}
