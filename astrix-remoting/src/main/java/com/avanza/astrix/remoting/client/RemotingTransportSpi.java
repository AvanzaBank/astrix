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
package com.avanza.astrix.remoting.client;

import java.util.Collection;
import java.util.List;

import com.avanza.astrix.core.remoting.RoutingKey;

import rx.Observable;
/**
 * SPI for implementing a RemotingTransport. <p>
 * 
 * A RemotingTransportSpi is responsible for interacting with remote service endpoints. That is, 
 * it can send remote service invocation request to the remote endpoint, and retrieve the corresponding
 * response.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface RemotingTransportSpi {
	
	/* 
	 * DESIGN NOTE:
	 * 
	 * The RemotingTransportSpi#submitRoutedRequests and RemotingTransportSpi#submitBroadcastRequest
	 * where initially designed to return an Observable that emitted one event for the response
	 * from each invocation (as opposed to emit a single event with a List of all responses).
	 * 
	 * The initial design caused problems when a subset of the service invocations did'nt return a response. 
	 * It seems that the HystrixObservableCommand's timeout mechanism only relates to the first emitted event. 
	 * As soon as one event is emitted (i.e a response from one service invocation is received), the timeout
	 * mechanism no longer applies and the service invocation won't timeout no matter how long it takes for the
	 * second event to be emitted. Therefore, in order to ensure that service invocation are protected
	 * with a timeout, the RemotingTransportSpi has been designed to only emit one event with all responses,
	 * or non at all.
	 */
	
	/**
	 * Send a single routed invocation request to the target cluster member.
	 *  
	 * @param request
	 * @param routingKey
	 * @return an Observable that will emit one item for the response from the given invocation request.
	 */
	Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey);
	
	/**
	 * Sends each service invocation to the associate target cluster member.
	 * 
	 * @param requests
	 * @return an Observable that will emit one item with the responses from each invocation
	 */
	Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(Collection<RoutedServiceInvocationRequest> requests);
	
	/**
	 * Sends a service invocation request to each member in the cluster. 
	 * 
	 * @param request
	 * @return an Observable that will emit one item with the responses from the invocation of each member in
	 * the entire cluster.
	 */
	Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(AstrixServiceInvocationRequest request);
	
	/**
	 * 
	 * @return the number of members in the target cluster.
	 */
	int partitionCount();
}