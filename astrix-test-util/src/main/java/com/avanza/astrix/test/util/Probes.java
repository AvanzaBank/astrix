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
package com.avanza.astrix.test.util;

import java.util.function.Supplier;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class Probes {
	
	public static Probe assertion(Runnable assertion) {
		return new Probe() {
			AssertionError lastAssertionError;
			@Override
			public void sample() {
				try {
					assertion.run();
				} catch (AssertionError e) {
					this.lastAssertionError = e;
				} catch (Exception e) {
					this.lastAssertionError = new AssertionError(e);
				}
			}
			@Override
			public boolean isSatisfied() {
				return lastAssertionError == null;
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("The assertion did not pass before timeout:\n");
				description.appendText(lastAssertionError.getMessage());
			}
		};
	}
	
	public static <T> Probe returnValue(Supplier<T> invocation, Matcher<? super T> returnValueMatcher) {
		return new Probe() {
			private T lastReturnedValue;
			@Override
			public void sample() {
				this.lastReturnedValue = invocation.get();
			}
			@Override
			public boolean isSatisfied() {
				return returnValueMatcher.matches(lastReturnedValue);
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected service invocation to return value equal to:\n");
				returnValueMatcher.describeTo(description);
				description.appendText("\nBut last returned value was: " + this.lastReturnedValue);
			}
		};
	}

	public static <T extends Exception> Probe expectThrows(Supplier<?> invocation, Matcher<T> returnValueMatcher) {
		return new Probe() {
			private Object lastReturnedValue;
			private Exception lastThrownException;
			@Override
			public void sample() {
				this.lastReturnedValue = null;
				this.lastThrownException = null;
				try {
					this.lastReturnedValue = invocation.get();
				} catch (Exception e) {
					lastThrownException = e;
				}
			}
			@Override
			public boolean isSatisfied() {
				return returnValueMatcher.matches(lastThrownException);
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected service invocation to throw:\n");
				returnValueMatcher.describeTo(description);
				if (lastReturnedValue != null) {
					description.appendText("\nBut last invocation returned: " + this.lastReturnedValue);
				} else {
					description.appendText("\nBut last invocation threw: " + this.lastThrownException);
				}
			}
		};
	}

}
