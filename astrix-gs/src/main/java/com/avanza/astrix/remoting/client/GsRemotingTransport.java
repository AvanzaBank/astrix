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
import java.util.List;

import org.openspaces.core.GigaSpace;

import rx.Observable;
import rx.Subscriber;

import com.avanza.astrix.remoting.client.AstrixRemotingTransport.Spi;
import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;

public class GsRemotingTransport implements Spi {

	private final GigaSpace gigaSpace;
	
	public GsRemotingTransport(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public Observable<AstrixServiceInvocationResponse> processRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		final AsyncFuture<AstrixServiceInvocationResponse> response = this.gigaSpace.execute(new AstrixServiceInvocationTask(request), routingKey);
		return Observable.create(new Observable.OnSubscribe<AstrixServiceInvocationResponse>() {
			@Override
			public void call(final Subscriber<? super AstrixServiceInvocationResponse> t1) {
				response.setListener(new AsyncFutureListener<AstrixServiceInvocationResponse>() {
					@Override
					public void onResult(AsyncResult<AstrixServiceInvocationResponse> result) {
						if (result.getException() == null) {
							t1.onNext(result.getResult());
							t1.onCompleted();
						} else {
							t1.onError(result.getException());
						}
					}
				});
			}
		});
	}
	
	@Override
	public Observable<List<AstrixServiceInvocationResponse>> processBroadcastRequest(AstrixServiceInvocationRequest request) {
		final AsyncFuture<List<AsyncResult<AstrixServiceInvocationResponse>>> responses = gigaSpace.execute(new AstrixDistributedServiceInvocationTask(request));
		return Observable.create(new Observable.OnSubscribe<List<AstrixServiceInvocationResponse>>() {
			@Override
			public void call(final Subscriber<? super List<AstrixServiceInvocationResponse>> subscriber) {
				responses.setListener(new AsyncFutureListener<List<AsyncResult<AstrixServiceInvocationResponse>>>() {
					@Override
					public void onResult(AsyncResult<List<AsyncResult<AstrixServiceInvocationResponse>>> asyncRresult) {
						if (asyncRresult.getException() == null) {
							List<AstrixServiceInvocationResponse> result = new ArrayList<>();
							for (AsyncResult<AstrixServiceInvocationResponse> asyncInvocationResponse : asyncRresult.getResult()) {
								result.add(asyncInvocationResponse.getResult());
							}
							subscriber.onNext(result);
							subscriber.onCompleted();
						} else {
							subscriber.onError(asyncRresult.getException());
						}
					}
				});
			}
		});
	}

	public static AstrixRemotingTransport remoteSpace(GigaSpace gigaSpace) {
		return new AstrixRemotingTransport(new GsRemotingTransport(gigaSpace));
	}
	
}