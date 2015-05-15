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
package com.avanza.astrix.remoting.client;

import java.util.List;

import rx.Observable;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixRemotingTransport {
	
	private final RemotingTransportSpi impl;
	
	AstrixRemotingTransport(RemotingTransportSpi impl) {
		this.impl = impl;
	}

	public static AstrixRemotingTransport create(RemotingTransportSpi impl) {
		return new AstrixRemotingTransport(impl);
	}

	public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		return impl.submitRoutedRequest(request, routingKey);
	}
	
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequests(List<RoutedServiceInvocationRequest> requests) {
		return impl.submitRoutedRequests(requests);
	}
	
	public Observable<AstrixServiceInvocationResponse> submitBroadcastRequest(AstrixServiceInvocationRequest request) {
		return impl.submitBroadcastRequest(request);
	}

	public int partitionCount() {
		return this.impl.partitionCount();
	}
	

}
