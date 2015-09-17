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
package com.avanza.astrix.remoting.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.async.internal.DefaultAsyncResult;

import rx.Observable;
import rx.functions.Func1;

public class GsUtilTest {

	@Test
	public void asyncResultList_ToFlatObservable_EmitsEachResultAsSingleItem() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>> asList(new DefaultAsyncResult<String>("foo", null),
				new DefaultAsyncResult<String>("bar", null));

		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		assertEquals(Arrays.asList("foo", "bar"), result.toList().toBlocking().first());
	}

	@Test
	public void asyncResultList_ToFlatObservable_Error() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>> asList(new DefaultAsyncResult<String>("foo", null),
				new DefaultAsyncResult<String>(null, new TestException()));

		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		Iterator<String> resultIterator = result.toBlocking().getIterator();
		resultIterator.next();
		try {
			resultIterator.next();
			fail("Expected TestException to be thrown");
		} catch (TestException e) {
			// expected
		}
	}

	public static class TestException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

}
