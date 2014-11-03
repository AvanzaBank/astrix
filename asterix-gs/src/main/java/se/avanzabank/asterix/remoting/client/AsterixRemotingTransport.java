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
package se.avanzabank.asterix.remoting.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openspaces.core.GigaSpace;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import se.avanzabank.asterix.remoting.server.AsterixServiceActivator;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixRemotingTransport {
	
	private final Spi impl;
	
	AsterixRemotingTransport(Spi impl) {
		this.impl = impl;
	}

	public static AsterixRemotingTransport remoteSpace(GigaSpace gigaSpace) {
		return new AsterixRemotingTransport(new GsImpl(gigaSpace));
	}
	
	public static AsterixRemotingTransport direct(AsterixServiceActivator activator) {
		return new AsterixRemotingTransport(new Direct(activator));
	}
	
	public static AsterixRemotingTransport create(Spi impl) {
		return new AsterixRemotingTransport(impl);
	}

	public AsterixServiceInvocationResponse processRoutedRequest(AsterixServiceInvocationRequest request, GsRoutingKey routingKey) {
		return impl.processRoutedRequest(request, routingKey).toBlockingObservable().first();
	}
	
	public List<AsterixServiceInvocationResponse> processBroadcastRequest(AsterixServiceInvocationRequest request) {
		List<AsterixServiceInvocationResponse> result = impl.processBroadcastRequest(request).toBlockingObservable().first();
		List<AsterixServiceInvocationResponse> responses = new ArrayList<>();
		for (AsterixServiceInvocationResponse asyncInvocationResponse : result) {
			responses.add(asyncInvocationResponse);
		}
		return responses;
	}
	
	public Observable<AsterixServiceInvocationResponse> observeProcessRoutedRequest(AsterixServiceInvocationRequest request, GsRoutingKey routingKey) {
		return impl.processRoutedRequest(request, routingKey);
	}
	
	public Observable<List<AsterixServiceInvocationResponse>> observeProcessBroadcastRequest(AsterixServiceInvocationRequest request) {
		return impl.processBroadcastRequest(request);
	}
	
	public interface Spi {
		public Observable<AsterixServiceInvocationResponse> processRoutedRequest(AsterixServiceInvocationRequest request, GsRoutingKey routingKey);
		public Observable<List<AsterixServiceInvocationResponse>> processBroadcastRequest(AsterixServiceInvocationRequest request);
	}
	
	private static class Direct implements Spi {

		private AsterixServiceActivator activator;

		public Direct(AsterixServiceActivator activator) {
			this.activator = activator;
		}

		@Override
		public Observable<AsterixServiceInvocationResponse> processRoutedRequest(AsterixServiceInvocationRequest request, GsRoutingKey routingKey){
			final AsterixServiceInvocationResponse response = activator.invokeService(request);
			return Observable.create(new OnSubscribeFunc<AsterixServiceInvocationResponse>() {
				@Override
				public Subscription onSubscribe(Observer<? super AsterixServiceInvocationResponse> t1) {
					t1.onNext(response);
					t1.onCompleted();
					return Subscriptions.empty();
				}
			});
		}

		@Override
		public Observable<List<AsterixServiceInvocationResponse>> processBroadcastRequest(AsterixServiceInvocationRequest request) {
			final AsterixServiceInvocationResponse response = activator.invokeService(request);
			return Observable.create(new OnSubscribeFunc<List<AsterixServiceInvocationResponse>>() {
				@Override
				public Subscription onSubscribe(Observer<? super List<AsterixServiceInvocationResponse>> t1) {
					t1.onNext(Arrays.asList(response));
					t1.onCompleted();
					return Subscriptions.empty();
				}
			});
		}
		
	}
	
	private static class GsImpl implements Spi {

		private final GigaSpace gigaSpace;
		
		public GsImpl(GigaSpace gigaSpace) {
			this.gigaSpace = gigaSpace;
		}

		@Override
		public Observable<AsterixServiceInvocationResponse> processRoutedRequest(AsterixServiceInvocationRequest request, GsRoutingKey routingKey) {
			final AsyncFuture<AsterixServiceInvocationResponse> response = this.gigaSpace.execute(new AsterixServiceInvocationTask(request), routingKey);
			return Observable.create(new OnSubscribeFunc<AsterixServiceInvocationResponse>() {
				@Override
				public Subscription onSubscribe(final Observer<? super AsterixServiceInvocationResponse> t1) {
					response.setListener(new AsyncFutureListener<AsterixServiceInvocationResponse>() {
						@Override
						public void onResult(AsyncResult<AsterixServiceInvocationResponse> result) {
							if (result.getException() == null) {
								t1.onNext(result.getResult());
								t1.onCompleted();
							} else {
								t1.onError(result.getException());
							}
						}
					});
					return Subscriptions.empty(); // TODO: subscription

				}
			});
		}
		
		@Override
		public Observable<List<AsterixServiceInvocationResponse>> processBroadcastRequest(AsterixServiceInvocationRequest request) {
			final AsyncFuture<List<AsyncResult<AsterixServiceInvocationResponse>>> responses = gigaSpace.execute(new AsterixDistributedServiceInvocationTask(request));
			return Observable.create(new OnSubscribeFunc<List<AsterixServiceInvocationResponse>>() {
				@Override
				public Subscription onSubscribe(final Observer<? super List<AsterixServiceInvocationResponse>> t1) {
					responses.setListener(new AsyncFutureListener<List<AsyncResult<AsterixServiceInvocationResponse>>>() {
						@Override
						public void onResult(AsyncResult<List<AsyncResult<AsterixServiceInvocationResponse>>> asyncRresult) {
							if (asyncRresult.getException() == null) {
								List<AsterixServiceInvocationResponse> result = new ArrayList<>();
								for (AsyncResult<AsterixServiceInvocationResponse> asyncInvocationResponse : asyncRresult.getResult()) {
									result.add(asyncInvocationResponse.getResult());
								}
								t1.onNext(result);
								t1.onCompleted();
							} else {
								t1.onError(asyncRresult.getException());
							}
						}
					});
					return Subscriptions.empty(); // TODO: subscription

				}
			});
		}
		
	}

	

}
