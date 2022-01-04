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
package com.avanza.astrix.gs.remoting;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.gs.SpaceTaskDispatcher;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;
import com.avanza.astrix.remoting.util.GsUtil;
import com.gigaspaces.admin.quiesce.QuiesceException;
import com.gigaspaces.async.AsyncResult;

import rx.Observable;
import rx.functions.Func1;
/**
 * RemotingTransport implementation based on GigaSpaces task execution. <p> 
 * 
 * @author Elias Lindholm
 *
 */
public class GsRemotingTransport implements RemotingTransportSpi {

	private final SpaceTaskDispatcher spaceTaskDispatcher;
	
	public GsRemotingTransport(SpaceTaskDispatcher spaceTaskDispatcher) {
		this.spaceTaskDispatcher = spaceTaskDispatcher;
	}
	
	@Override
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(final AstrixServiceInvocationRequest request, final RoutingKey routingKey) {
		return observeRoutedRequest(request, routingKey);
	}

	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(final Collection<RoutedServiceInvocationRequest> requests) {
		if (requests.isEmpty()) {
			return Observable.just(Collections.<AstrixServiceInvocationResponse>emptyList());
		}
		return observeRoutedReqeuests(requests);
	}
	
	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(final AstrixServiceInvocationRequest request) {
		return observeBroadcastRequest(request);
	}
	
	private Observable<AstrixServiceInvocationResponse> observeRoutedRequest(AstrixServiceInvocationRequest request,
																			  RoutingKey routingKey) {
		return spaceTaskDispatcher.observe(new AstrixServiceInvocationTask(request), routingKey)
				.onErrorResumeNext(this::translateToObservableError);
	}
	
	private Observable<List<AstrixServiceInvocationResponse>> observeRoutedReqeuests(Collection<RoutedServiceInvocationRequest> requests) {
		Observable<AstrixServiceInvocationResponse> result = Observable.empty();
		for (RoutedServiceInvocationRequest request : requests) {
			result = result.mergeWith(spaceTaskDispatcher.observe(new AstrixServiceInvocationTask(request.getRequest()), request.getRoutingkey()));
		}
		return result.toList()
				.onErrorResumeNext(this::translateToObservableError);
	}
	
	private Observable<List<AstrixServiceInvocationResponse>> observeBroadcastRequest(AstrixServiceInvocationRequest request) {
		Observable<List<AsyncResult<AstrixServiceInvocationResponse>>> responses = spaceTaskDispatcher.observe(new AstrixDistributedServiceInvocationTask(request));
		Func1<List<AsyncResult<AstrixServiceInvocationResponse>>, Observable<AstrixServiceInvocationResponse>> listToObservable = 
				GsUtil.asyncResultListToObservable();
		Observable<AstrixServiceInvocationResponse> responseStream = responses.flatMap(listToObservable);
		return responseStream.toList()
				.onErrorResumeNext(this::translateToObservableError);
	}

	private <T> Observable<T> translateToObservableError(Throwable e) {
		return Observable.error(translateException(e));
	}

	private Throwable translateException(Throwable e) {
		if (e instanceof QuiesceException) {
			return new ServiceUnavailableException(e.getMessage(), e);
		}
		return e;
	}

	@Override
	public int partitionCount() {
		return this.spaceTaskDispatcher.partitionCount();
	}
	
}
