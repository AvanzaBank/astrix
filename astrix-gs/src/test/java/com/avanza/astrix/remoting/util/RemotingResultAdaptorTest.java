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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingResult;

import com.avanza.astrix.core.AstrixRemoteResult;

public class RemotingResultAdaptorTest {

	@Test
	public void getResult_astrixRemotingResultWrap() throws Throwable {
		assertEquals("hej", 
				RemotingResultAdaptor.AstrixRemotingResultWrap.wrap(Arrays.asList(new AstrixRemoteResult<>("hej", null))).iterator().next().getResult());
	}
	
	@Test
	public void getResult_gigaSpaceRemotingResultWrap() throws Throwable {
		@SuppressWarnings("unchecked")
		Collection<RemotingResultAdaptor<?>> wrap = (Collection<RemotingResultAdaptor<?>>) 
			RemotingResultAdaptor.GigaSpaceRemotingResultWrap.wrap(new SpaceRemotingResult[] { new MySpaceRemotingResult<String>("hej", null) });
		assertEquals("hej", wrap.iterator().next().getResult());
	}
	
	@Test(expected = MyException.class)
	public void getResult_astrixRemotingResultWrap_rethrowsException() throws Throwable {
		RemotingResultAdaptor.AstrixRemotingResultWrap.wrap(Arrays.asList(new AstrixRemoteResult<>(null, new MyException()))).iterator().next().getResult();
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = MyException.class)
	public void getResult_gigaSpaceRemotingResultWrap_rethrowsException() throws Throwable {
		((Collection<RemotingResultAdaptor<?>>) RemotingResultAdaptor.GigaSpaceRemotingResultWrap.wrap(new SpaceRemotingResult[] { new MySpaceRemotingResult<String>(null, new MyException()) }))
			.iterator().next().getResult();
	}
	
	public static class MySpaceRemotingResult<T> implements SpaceRemotingResult<T> {
		private T result;
		private Throwable exception;

		public MySpaceRemotingResult(T result, Throwable exception) {
			this.result = result;
			this.exception = exception;
		}

		@Override
		public Integer getRouting() {
			return 1;
		}

		@Override
		public T getResult() {
			return result;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public Integer getInstanceId() {
			return null;
		}
	}
	
	public static class MyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
}
