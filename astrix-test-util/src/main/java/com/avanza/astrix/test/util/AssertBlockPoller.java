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

public class AssertBlockPoller {

	private final long timeoutMillis;
	private final long pollDelayMillis;
	
	public AssertBlockPoller(long timeoutMillis, long pollDelayMillis) {
		this.timeoutMillis = timeoutMillis;
		this.pollDelayMillis = pollDelayMillis;
	}

	public void check(Runnable assertion) throws InterruptedException {
		Timeout timeout = newTimeout(timeoutMillis);
		AssertionError lastAssertionError = new AssertionError("Did check assertion before timeout");
		while (lastAssertionError != null) {
			if (timeout.hasTimeout()) {
				throw lastAssertionError;
			}
			try {
				assertion.run();
				lastAssertionError = null;
			} catch (AssertionError error) {
				lastAssertionError = error;
			}
			Thread.sleep(pollDelayMillis);
		}
	}

	Timeout newTimeout(long timeout) {
		return new Timeout(timeout);
	}


}
