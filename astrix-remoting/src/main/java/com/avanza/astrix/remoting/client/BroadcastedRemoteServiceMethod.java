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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.AstrixRemoteResultReducer;
import com.avanza.astrix.core.util.ReflectionUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
	
public class BroadcastedRemoteServiceMethod implements RemoteServiceMethod {
	
	private final String signature;
	private final Class<? extends AstrixRemoteResultReducer> reducer;
	private final RemotingEngine remotingEngine;
	private final Type returnType;
	
	public BroadcastedRemoteServiceMethod(String signature,
			Class<? extends AstrixRemoteResultReducer> reducer,
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
	
	private AstrixRemoteResultReducer<?, ?> newReducer() {
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
		final AstrixRemoteResultReducer<T, T> reducer = (AstrixRemoteResultReducer<T, T>) newReducer();
		Observable<List<AstrixServiceInvocationResponse>> responesObservable = remotingEngine.processBroadcastRequest(request).toList();
		if (returnType.equals(Void.TYPE)) {
			return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
				@Override
				public T call(List<AstrixServiceInvocationResponse> t1) {
					return null;
				}
			});
		}
		return responesObservable.map(new Func1<List<AstrixServiceInvocationResponse>, T>() {
			@Override
			public T call(List<AstrixServiceInvocationResponse> t1) {
				List<AstrixRemoteResult<T>> unmarshalledResponses = new ArrayList<>();
				for (AstrixServiceInvocationResponse response : t1) {
					AstrixRemoteResult<T> result = remotingEngine.toRemoteResult(response, returnType);
					unmarshalledResponses.add(result);
				}
				return reducer.reduce(unmarshalledResponses);
			}
		});
	}
	
}