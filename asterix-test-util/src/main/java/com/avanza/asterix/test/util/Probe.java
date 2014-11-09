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

import org.hamcrest.Description;

/**
 * Used in combination with a Poller to test asynchronous code. More specifically to 
 * wait for a given condition to be satisfied by polling a given resource. <p>
 * 
 * See Poller for usage.<p>
 * 
 * 
 * @author Elias Lindholm
 *
 */
public interface Probe {
	
	/**
	 * Indicates whether this Probe is satisfied or note, will be called repeatedly
	 * until timeout occurs. Timeout is determine by the Poller that polls the probe. <p>
	 * 
	 * @return
	 */
	boolean isSatisfied();
	
	/**
	 * Sample the given resource. After each sample the isSatisfied() method will be queried
	 * to see whether to condition i yet satisfied. <p> 
	 */
	void sample();
	
	/**
	 * This method will be invoked in case of failure, i.e. the probe was not satisfied before
	 * timeout occurs. <p>
	 * 
	 * @param description
	 */
	void describeFailureTo(Description description);

}
