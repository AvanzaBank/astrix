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
package com.avanza.astrix.beans.tracing;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface InvocationExecutionWatcher {
	/**
	 * Called just before the actual rpc method invocation.
	 *
	 * @return A watch that will be notified just after the call has finished executing
	 */
	AfterInvocation beforeInvocation(Map<String, String> headers);

	@FunctionalInterface
	interface AfterInvocation {
		/**
		 * Called just after the rpc method invocation
		 */
		void afterInvocation();
	}

	static Runnable apply(List<InvocationExecutionWatcher> watchers, Map<String, String> headers) {
		List<AfterInvocation> after = watchers.stream()
				.map(w -> w.beforeInvocation(headers))
				.collect(toList());
		return () -> after.forEach(AfterInvocation::afterInvocation);
	}
}
