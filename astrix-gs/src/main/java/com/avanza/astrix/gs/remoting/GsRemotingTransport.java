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
package com.avanza.astrix.gs.remoting;

import java.util.Collection;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.ft.AstrixFaultTolerance;
import com.avanza.astrix.ft.ObservableCommandSettings;
import com.avanza.astrix.gs.SpaceTaskDispatcher;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;
import com.avanza.astrix.remoting.client.RoutingKey;
import com.avanza.astrix.remoting.util.GsUtil;
import com.gigaspaces.async.AsyncResult;
/**
 * RemotingTransport implementation based on GigaSpaces task execution. <p> 
 * 
 * @author Elias Lindholm
 *
 */
public class GsRemotingTransport implements RemotingTransportSpi {

	private final SpaceTaskDispatcher spaceTaskDispatcher;
	private final AstrixFaultTolerance faultTolerance;
	
	public GsRemotingTransport(SpaceTaskDispatcher spaceTaskDispatcher, AstrixFaultTolerance faultTolerance) {
		this.spaceTaskDispatcher = spaceTaskDispatcher;
		this.faultTolerance = faultTolerance;
	}
	
	@Override
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(final AstrixServiceInvocationRequest request, final RoutingKey routingKey) {
		return faultTolerance.observe(new Supplier<Observable<AstrixServiceInvocationResponse>>() {
			@Override
			public Observable<AstrixServiceInvocationResponse> get() {
				return executeRoutedRequest(request, routingKey);
			}

		}, getServiceCommandSettings(request));
	}

	@Override
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequests(final Collection<RoutedServiceInvocationRequest> requests) {
		if (requests.isEmpty()) {
			return Observable.empty();
		}
		return faultTolerance.observe(new Supplier<Observable<AstrixServiceInvocationResponse>>() {
			@Override
			public Observable<AstrixServiceInvocationResponse> get() {
				return observeRoutedReqeuests(requests);
			}

		}, getServiceCommandSettings(requests.iterator().next().getRequest()));

	}
	
	
	@Override
	public Observable<AstrixServiceInvocationResponse> submitBroadcastRequest(final AstrixServiceInvocationRequest request) {
		return faultTolerance.observe(new Supplier<Observable<AstrixServiceInvocationResponse>>() {
			@Override
			public Observable<AstrixServiceInvocationResponse> get() {
				return executeBroadcastRequest(request);
			}

		}, getServiceCommandSettings(request));
	}
	
	private Observable<AstrixServiceInvocationResponse> executeRoutedRequest(AstrixServiceInvocationRequest request,
																			  RoutingKey routingKey) {
		return spaceTaskDispatcher.observe(new AstrixServiceInvocationTask(request), routingKey);
	}
	
	private Observable<AstrixServiceInvocationResponse> observeRoutedReqeuests(Collection<RoutedServiceInvocationRequest> requests) {
		Observable<AstrixServiceInvocationResponse> result = Observable.empty();
		for (RoutedServiceInvocationRequest request : requests) {
			result = result.mergeWith(spaceTaskDispatcher.observe(new AstrixServiceInvocationTask(request.getRequest()), request.getRoutingkey()));
		}
		return result;
	}
	
	private Observable<AstrixServiceInvocationResponse> executeBroadcastRequest(AstrixServiceInvocationRequest request) {
		Observable<List<AsyncResult<AstrixServiceInvocationResponse>>> responses = spaceTaskDispatcher.observe(new AstrixDistributedServiceInvocationTask(request));
		Func1<List<AsyncResult<AstrixServiceInvocationResponse>>, Observable<AstrixServiceInvocationResponse>> listToObservable = 
				GsUtil.asyncResultListToObservable();
		Observable<AstrixServiceInvocationResponse> responseStream = responses.flatMap(listToObservable);
		return responseStream;
	}

	private ObservableCommandSettings getServiceCommandSettings(AstrixServiceInvocationRequest request) {
		String api = request.getHeader(AstrixServiceInvocationRequestHeaders.SERVICE_API);
		String spaceName = spaceTaskDispatcher.getSpaceName();
		String[] subPackagesAndClassName = api.split("[.]");
		if (subPackagesAndClassName.length > 0) {
			api = subPackagesAndClassName[subPackagesAndClassName.length - 1]; // Use simple class name without package name
		}
		String commandKey = spaceName + "_" + api;
		ObservableCommandSettings commandSettings = new ObservableCommandSettings(commandKey, spaceTaskDispatcher.getSpaceName());
		return commandSettings;
	}

	@Override
	public int partitionCount() {
		return this.spaceTaskDispatcher.partitionCount();
	}
	
}