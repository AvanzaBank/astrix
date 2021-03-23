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
import java.util.ArrayList;
import java.util.List;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.RemoteResultReducer;
import com.avanza.astrix.core.util.ReflectionUtil;

import rx.Observable;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
	
public class BroadcastedRemoteServiceMethod implements RemoteServiceMethod {
	
	private final String signature;
	private final Class<? extends RemoteResultReducer> reducer;
	private final RemotingEngine remotingEngine;
	private final Type returnType;
	
	public BroadcastedRemoteServiceMethod(String signature,
			Class<? extends RemoteResultReducer> reducer,
			RemotingEngine remotingEngine, 
			Type returnType) {
		this.signature = signature;
		this.reducer = reducer;
		this.remotingEngine = remotingEngine;
		this.returnType = returnType;
	}

	public String getSignature() {
		return signature;
	}
	
	private RemoteResultReducer<?> newReducer() {
		return ReflectionUtil.newInstance(this.reducer);
	}

	@Override
	public Observable<?> invoke(AstrixServiceInvocationRequest invocationRequest, Object[] args) throws Exception {
		return submitBroadcastRequest(invocationRequest, args);
	}
	
	private <T> Observable<T> submitBroadcastRequest(
			AstrixServiceInvocationRequest request, Object[] args) throws InstantiationException,
			IllegalAccessException {
		request.setArguments(remotingEngine.marshall(args));
		@SuppressWarnings("unchecked")
		final RemoteResultReducer<T> reducer = (RemoteResultReducer<T>) newReducer();
		Observable<List<AstrixServiceInvocationResponse>> responesObservable = remotingEngine.submitBroadcastRequest(request);
		if (returnType.equals(Void.TYPE) || returnType.equals(Void.class)) {
			return responesObservable.map(responses -> {
				readResponses(responses);
				return null;
			});
		}
		return responesObservable.map(responses -> {
			List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>();
			for (AstrixServiceInvocationResponse response : responses) {
				AstrixRemoteResult<T> result = remotingEngine.toRemoteResult(response, returnType);
				unmarshalledResponses.add(result);
			}
			return reducer.reduce(unmarshalledResponses);
		});
	}

	private void readResponses(List<AstrixServiceInvocationResponse> responses) {
		responses.forEach(res -> remotingEngine.toRemoteResult(res, returnType).getResult());
	}

}