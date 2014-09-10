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
package se.avanzabank.asterix.ft;

import se.avanzabank.asterix.core.ServiceUnavailableException;

import com.netflix.hystrix.HystrixCommand;

final class AsterixUtil {
	
	static ServiceUnavailableCause resolveUnavailableCause(HystrixCommand<?> executable) {
		if (executable.isResponseRejected()) {
			return ServiceUnavailableCause.REJECTED_EXECUTION;
		}
		if (executable.isResponseTimedOut()) {
			return ServiceUnavailableCause.TIMEOUT;
		}
		if (executable.isResponseShortCircuited()) {
			return ServiceUnavailableCause.SHORT_CIRCUITED;
		}
		if (executable.isFailedExecution()) {
			return ServiceUnavailableCause.UNAVAILABLE;
		}
		return ServiceUnavailableCause.UNKNOWN;
	}
	
	static ServiceUnavailableException wrapFailedExecutionException(HystrixCommand<?> executable) {
		Throwable failedExecutionException = executable.getFailedExecutionException();
		if (failedExecutionException instanceof ServiceUnavailableException) {
			return (ServiceUnavailableException) failedExecutionException;
		}
		return new ServiceUnavailableException("Wrapper for exception thrown from invoke(api)", failedExecutionException);
	}

}