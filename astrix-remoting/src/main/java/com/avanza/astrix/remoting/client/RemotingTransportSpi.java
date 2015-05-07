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
 * SPI for implementing a RemotingTransport. <p>
 * 
 * A RemotingTransportSpi is repsonsible for sending a ServiceInvocationRequest to a remote endpoint
 * and receive a repoy from the ServiceInvocationRequest.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface RemotingTransportSpi {
	Observable<AstrixServiceInvocationResponse> processRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey);
	Observable<List<AstrixServiceInvocationResponse>> processBroadcastRequest(AstrixServiceInvocationRequest request);
	int partitionCount();
}