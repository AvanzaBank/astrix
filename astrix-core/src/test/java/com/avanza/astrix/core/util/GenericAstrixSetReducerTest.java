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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.ServiceInvocationException;


public class GenericAstrixSetReducerTest {

    private GenericAstrixSetReducer<String> reducer;

    @Before
    public void setup() {
        reducer = new GenericAstrixSetReducer<String>();
    }

    @Test
    public void reducedSetContainsUniqueElements() {
        Set<String> first = Collections.singleton("1");
        Set<String> second = Collections.singleton("1");
        Set<String> third = Collections.singleton("2");

        Set<String> reduced = reducer.reduce(Arrays.<AstrixRemoteResult<Set<String>>>asList(
                AstrixRemoteResult.successful(first),
                AstrixRemoteResult.successful(second),
                AstrixRemoteResult.successful(third)));

        assertThat(reduced.size(), is(2));
        assertTrue(reduced.containsAll(first));
        assertTrue(reduced.containsAll(third));
    }

	@Test(expected = MyRuntimeException.class)
	public void exceptionIsRethrown() throws Exception {
		reducer.reduce(Arrays.asList(
                AstrixRemoteResult.successful(Collections.singleton("hello world")),
				AstrixRemoteResult.<Set<String>>failure(new MyRuntimeException(), CorrelationId.undefined())));
	}

	private static class MyRuntimeException extends ServiceInvocationException {
		private static final long serialVersionUID = 1L;
	}
}
