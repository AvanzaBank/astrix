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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

import com.avanza.astrix.remoting.server.AstrixServiceActivator;
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

	public static AstrixRemotingTransport direct(AstrixServiceActivator activator) {
		return new AstrixRemotingTransport(new Direct(activator));
	}
	
	public static AstrixRemotingTransport create(RemotingTransportSpi impl) {
		return new AstrixRemotingTransport(impl);
	}

	public AstrixServiceInvocationResponse processRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		return impl.processRoutedRequest(request, routingKey).toBlocking().first();
	}
	
	public List<AstrixServiceInvocationResponse> processBroadcastRequest(AstrixServiceInvocationRequest request) {
		List<AstrixServiceInvocationResponse> result = impl.processBroadcastRequest(request).toBlocking().first();
		List<AstrixServiceInvocationResponse> responses = new ArrayList<>();
		for (AstrixServiceInvocationResponse asyncInvocationResponse : result) {
			responses.add(asyncInvocationResponse);
		}
		return responses;
	}
	
	public Observable<AstrixServiceInvocationResponse> observeProcessRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		return impl.processRoutedRequest(request, routingKey);
	}
	
	public Observable<List<AstrixServiceInvocationResponse>> observeProcessBroadcastRequest(AstrixServiceInvocationRequest request) {
		return impl.processBroadcastRequest(request);
	}
	
	private static class Direct implements RemotingTransportSpi {

		private AstrixServiceActivator activator;

		public Direct(AstrixServiceActivator activator) {
			this.activator = activator;
		}

		@Override
		public Observable<AstrixServiceInvocationResponse> processRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey){
			final AstrixServiceInvocationResponse response = activator.invokeService(request);
			return Observable.create(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(Subscriber<? super AstrixServiceInvocationResponse> t1) {
					t1.onNext(response);
					t1.onCompleted();
				}
			});
		}

		@Override
		public Observable<List<AstrixServiceInvocationResponse>> processBroadcastRequest(AstrixServiceInvocationRequest request) {
			final AstrixServiceInvocationResponse response = activator.invokeService(request);
			return Observable.create(new Observable.OnSubscribe<List<AstrixServiceInvocationResponse>>() {
				@Override
				public void call(Subscriber<? super List<AstrixServiceInvocationResponse>> t1) {
					t1.onNext(Arrays.asList(response));
					t1.onCompleted();
				}
			});
		}
		
	}

	

}
