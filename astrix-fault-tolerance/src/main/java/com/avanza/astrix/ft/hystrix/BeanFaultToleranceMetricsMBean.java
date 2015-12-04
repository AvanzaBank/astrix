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
package com.avanza.astrix.ft.hystrix;

public interface BeanFaultToleranceMetricsMBean {
	/*
	 
	 #COMMAND METRICS
	 
	  EXCEPTION_THROWN?
	  FAILURE?
	  
	  SERVICE_UNAVAILABLE 
	  
	  SEMAPHORE_REJECTED -> semaphoreRejectedCount
	  SHORT_CIRCUITED -> shortCircuitedCount
	  TIMEOUT -> timeoutCount
	  THREAD_POOL_REJECTED -> threadPoolRejectedCount
	  	  
	  SUCCESS - -> successCount
	  
	  
	  currentConcurrentExcecutionCount
	  rollingMaxConcurrentExecutions
	  
	  
	  #POOL METRICS
	  
	  currentActiveCount -> poolCurrentActiveCount
	  currentQueueSize	-> poolCurrentQueueSize  
	  rollingMaxActiveThreads -> poolRollingMaxActiveThreads

	 */

	/**
	 * The number of failed invocations to a given service bean, i.e
	 * the number of times the fault tolerance layer has aborted the
	 * invocation (sempahore rejected, timeout, etc)
	 * @return
	 */
	long getErrorCount();

	int getErrorPercentage();

	/**
	 * The number of successful invocations of a given service bean. <p>
	 * @return
	 */
	long getSuccessCount();
	
	int getCurrentConcurrentExecutionCount();

	long getRollingMaxConcurrentExecutions();

	long getThreadPoolRejectedCount();

	long getTimeoutCount();

	long getShortCircuitedCount();

	long getSemaphoreRejectedCount();

	int getPoolCurrentActiveCount();

	int getPoolCurrentQueueCount();

	int getPoolRollingMaxActiveThreads();
	

}