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
package com.avanza.astrix.netty;

import java.util.Collection;
import java.util.List;

import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.netty.client.NettyRemotingClient;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;

import rx.Observable;

public class NettyRemotingTransport implements RemotingTransportSpi {
	
	private NettyRemotingClient remotingClient;

	public NettyRemotingTransport(NettyRemotingClient remotingClient) {
		this.remotingClient = remotingClient;
	}

	@Override
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		return remotingClient.invokeService(request); // TODO: Add support for routing
	}

	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(Collection<RoutedServiceInvocationRequest> requests) {
		Observable<AstrixServiceInvocationResponse> result = submitRoutedRequest(requests.iterator().next().getRequest(), null);
		return result.toList();
	}

	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(AstrixServiceInvocationRequest request) {
		Observable<AstrixServiceInvocationResponse> result = submitRoutedRequest(request, null);
		return result.toList();
	}

	@Override
	public int partitionCount() {
		return 1;
	}

}
