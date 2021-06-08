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
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class GenericAstrixSetReducerTest {

    private final GenericAstrixSetReducer<String> reducer = new GenericAstrixSetReducer<>();

    @Test
    void reducedSetContainsUniqueElements() {
        Set<String> first = singleton("1");
        Set<String> second = singleton("1");
        Set<String> third = singleton("2");

        Set<String> reduced = reducer.reduce(Arrays.asList(
                AstrixRemoteResult.successful(first),
                AstrixRemoteResult.successful(second),
                AstrixRemoteResult.successful(third)));

        assertEquals(reduced.size(), 2);
        assertTrue(reduced.containsAll(first));
        assertTrue(reduced.containsAll(third));
    }

	@Test
    void exceptionIsRethrown() {
		assertThrows(MyRuntimeException.class, () -> reducer.reduce(Arrays.asList(
                AstrixRemoteResult.successful(singleton("hello world")),
				AstrixRemoteResult.failure(new MyRuntimeException(), CorrelationId.undefined()))));
	}

	private static class MyRuntimeException extends ServiceInvocationException {
		private static final long serialVersionUID = 1L;
	}
}
