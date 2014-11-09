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
package com.avanza.asterix.test.util;

import org.hamcrest.StringDescription;

/**
 * A Poller polls a given Probe until is is satisfied or timeout occurs.
 * 
 * Usage:
 * 
 * <pre>
 * 		
 * 		assertEventually(fileLength("data.txt", is(greaterThan(2000))))
 * 
 * 
 * 		// 
 * 		public static void assertEventually(Probe probe) {
 * 			return new Poller(1000L, 50L).check(probe); // 1 second timeout, checks probe every 50 ms unitl timeout.
 * 		}
 * 
 * 		public static Probe fileLength(String file, Matcher<Integer> matcher) {
 * 			return new Probe() {
 * 				int lastFileLength;
 * 
 * 				public boolean isSatisfied() {
 * 					return matcher.matches(lastFileLength);
 * 				}
 * 
 * 				public void sample() {
 * 					lastFileLength = new File(1000L).length();
 * 				}
 * 
 * 				public void describeFailureOf(Description d) {
 * 					d.appendText("length was " + lastFileLength);
 * 				}
 * 			}
 * 		}
 * 
 * 		
 *
 *
 *
 *
 *
 * </pre>
 * 
 * This class is based on a design found in Growing Object Oriented Software Guided by test.
 * See chapter about testing asynchronous code for a discussion about when to use this class. <p>
 * 
 * @author Elias Lindholm
 *
 */
public class Poller {
	
	private long timeoutMillis;
	private long pollDelayMillis;
	
	public Poller(long timeoutMillis, long pollDelayMillis) {
		this.timeoutMillis = timeoutMillis;
		this.pollDelayMillis = pollDelayMillis;
	}

	/**
	 * 
	 * Polls the given probe until it is satisfied or timeout occurs. <p>
	 * 
	 * This method will return as soon as the Probe is satisfied. <p>
	 * 
	 * If timeout occurs an AssertionError will be thrown.<p>
	 * 
	 * @param probe
	 * @throws InterruptedException - if the current thread is interrupted.
	 * @throws AssertionError - if timeout occurs before the Probe is satisfied.
	 */
	public void check(Probe probe) throws InterruptedException {
		Timeout timeout = newTimeout(timeoutMillis);
		probe.sample();
		while (!probe.isSatisfied()) {
			if (timeout.hasTimeout()) {
				throw new AssertionError(describeFailureOf(probe));
			}
			sleep(pollDelayMillis);
			probe.sample();
		}
	}

	// test hook
	Timeout newTimeout(long timeout) {
		return new Timeout(timeout);
	}

	// test hook
	void sleep(long sleepTimeMillis) throws InterruptedException {
		Thread.sleep(sleepTimeMillis);
	}

	private String describeFailureOf(Probe probe) {
		StringDescription description = new StringDescription();
		probe.describeFailureTo(description);
		return description.toString();
	}

}
