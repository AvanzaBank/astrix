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
package se.avanzabank.service.suite.core;


public class AstrixRemoteResult<T> {
	
	private final T result;
	private final RuntimeException exception;

	public AstrixRemoteResult(T result, RuntimeException exception) {
		this.result = result;
		this.exception = exception;
	}
	
	public static <T> AstrixRemoteResult<T> successful(T result) {
		return new AstrixRemoteResult<>(result, null);
	}
	
	public static <T> AstrixRemoteResult<T> failure(RuntimeException exception) {
		return new AstrixRemoteResult<>(null, exception);
	}
	
	public T getResult() {
		if (hasThrownException()) {
			appendCurrentCallStackToThrowable(exception);
			throw exception;
		}
		return result;
	}
	
	/*
	 * Since the execution of an AstrixCommand does not contain the call-stack where the command were created and invoked, 
	 * we append the current callstack to the exception for increased tracability.
	 */
	private static void appendCurrentCallStackToThrowable(Throwable throwable) {
		if (throwable.getCause() == null) {
			throwable.initCause(new AstrixCallStackTrace());
		} else {
			appendCurrentCallStackToThrowable(throwable.getCause());
		}
	}

	public boolean hasThrownException() {
		return exception != null;
	}

	public static <T> AstrixRemoteResult<T> voidResult() {
		return new AstrixRemoteResult<>(null, null);
	}

}
