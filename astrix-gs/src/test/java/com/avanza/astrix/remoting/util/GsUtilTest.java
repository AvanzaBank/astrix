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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Objects;

import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingResult;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.ServiceInvocationException;

public class GsUtilTest {

	@SuppressWarnings("unchecked")
	@Test
	public void convertToAstrixRemoteResults_gigaSpaceRemotingResultWrap() throws Throwable {
		assertEquals(Arrays.asList(AstrixRemoteResult.successful("hej")), 
				GsUtil.<String>convertToAstrixRemoteResults(new SpaceRemotingResult[] { new MySpaceRemotingResult<String>("hej", null) }));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void convertToAstrixRemoteResults_gigaSpaceRemotingResultWrap_retainsException() throws Throwable {
		assertEquals(Arrays.asList(AstrixRemoteResult.failure(new MyException())), 
				GsUtil.<String>convertToAstrixRemoteResults(new SpaceRemotingResult[] { new MySpaceRemotingResult<String>(null, new MyException()) }));
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
	
	public static class MyException extends ServiceInvocationException {
		public MyException(CorrelationId correlationId) {
			super(correlationId);
		}
		
		public MyException() {
			super(UNDEFINED_CORRELACTION_ID);
		}

		private static final long serialVersionUID = 1L;
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(1);
		}
		
		@Override
		public ServiceInvocationException reCreateOnClientSide(CorrelationId correlationId) {
			return new MyException(correlationId);
		}
	}
	
}
