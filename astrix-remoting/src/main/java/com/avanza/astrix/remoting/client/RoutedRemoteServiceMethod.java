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

import java.lang.reflect.Type;

import rx.Observable;
import rx.functions.Func1;

public class RoutedRemoteServiceMethod implements RemoteServiceMethod {

	private final String signature;
	private final Router router;
	private final RemotingEngine remotingEngine;
	private final Type returnType;

	public RoutedRemoteServiceMethod(String signature, 
									 Router router,
									 RemotingEngine remotingEngine, 
									 Type returnType) {
		this.signature = signature;
		this.router = router;
		this.remotingEngine = remotingEngine;
		this.returnType = returnType;
	}

	public String getSignature() {
		return signature;
	}
	
	private RoutingKey getRoutingKey(Object... args) throws Exception {
		return this.router.getRoutingKey(args);
	}
	
	@Override
	public Observable<?> invoke(AstrixServiceInvocationRequest invocationRequest, Object[] args) throws Exception {
		invocationRequest.setArguments(remotingEngine.marshall(args));
		RoutingKey routingKey = router.getRoutingKey(args);
		if (routingKey == null) {
			throw new IllegalStateException(String.format("Service method is routed but the defined remotingKey value was null: method=%s", signature));
		}
		return submitRoutedRequest(invocationRequest, routingKey);
	}
	
	protected Observable<Object> submitRoutedRequest(AstrixServiceInvocationRequest request,
			RoutingKey routingKey) {
		Observable<AstrixServiceInvocationResponse> response = remotingEngine.submitRoutedRequest(
				request, routingKey);
		return response.map(new Func1<AstrixServiceInvocationResponse, Object>() {
			@Override
			public Object call(AstrixServiceInvocationResponse t1) {
				return remotingEngine.toRemoteResult(t1, returnType).getResult();
			}
		});
	}
	
}
