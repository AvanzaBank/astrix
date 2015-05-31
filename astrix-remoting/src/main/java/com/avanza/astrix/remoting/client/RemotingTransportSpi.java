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
	 * @return an Observable that will emit one item for the response for each routed invocation request.
	 */
	Observable<AstrixServiceInvocationResponse> submitRoutedRequests(Collection<RoutedServiceInvocationRequest> requests);
	
	/**
	 * Sends a service invocation request to each member in the cluster. 
	 * 
	 * @param request
	 * @return an Observable that will emit one item for the response for the invocation of each member in
	 * the entire cluster.
	 */
	Observable<AstrixServiceInvocationResponse> submitBroadcastRequest(AstrixServiceInvocationRequest request);
	
	/**
	 * 
	 * @return the number of members in the target cluster.
	 */
	int partitionCount();
}