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
package com.avanza.astrix.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.RemotingTransportSpi;
import com.avanza.astrix.remoting.client.RoutedServiceInvocationRequest;
import com.avanza.astrix.remoting.client.RoutingKey;
/**
 * 
 * @author Elias Lindholm
 *
 */
public final class HttpRemotingTransport implements RemotingTransportSpi {

	private final CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
	private final Map<Integer, ClusterMember> clusterMembers = new ConcurrentHashMap<>();
	private final int clusterSize; // may be larger than clusterMembers.size in case not all members are discovered yet.
	
	public HttpRemotingTransport(Collection<ClusterMember> clusterMembers, int clusterSize) {
		this.clusterSize = clusterSize;
		for (ClusterMember clusterMember : clusterMembers) {
			this.clusterMembers.put(clusterMember.getClusterInstanceId(), clusterMember);
		}
	}
	
	@Override
	public Observable<AstrixServiceInvocationResponse> submitRoutedRequest(
			final AstrixServiceInvocationRequest request, final RoutingKey routingKey) {
		ClusterMember clusterMember = getTargetMember(routingKey);
		final HttpPost postRequest = new HttpPost(clusterMember.getRemoteEndpointUri());
		postRequest.setEntity(new SerializableEntity(request));
		return Observable.create(new OnSubscribe<AstrixServiceInvocationResponse>() {
			@Override
			public void call(final Subscriber<? super AstrixServiceInvocationResponse> t1) {
				try {
					httpclient.execute(postRequest, serviceResponseCallback(t1));
				} catch (Exception e) {
					t1.onError(e);
				}
			}

		});
	}
	
	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(
			Collection<RoutedServiceInvocationRequest> requests) {
		Observable<AstrixServiceInvocationResponse> result = Observable.empty();
		for (RoutedServiceInvocationRequest request : requests) {
			result = result.mergeWith(submitRoutedRequest(request.getRequest(), request.getRoutingkey()));
		}
		return result.toList();
	}

	@Override
	public Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(
			AstrixServiceInvocationRequest request) {
		Observable<AstrixServiceInvocationResponse> result = Observable.empty();
		for (ClusterMember clusterMember : getAllClusterMembers()) {
			final HttpPost postRequest = new HttpPost(clusterMember.getRemoteEndpointUri());
			postRequest.setEntity(new SerializableEntity(request));
			result = result.mergeWith(Observable.create(new OnSubscribe<AstrixServiceInvocationResponse>() {
				@Override
				public void call(final Subscriber<? super AstrixServiceInvocationResponse> t1) {
					try {
						httpclient.execute(postRequest, serviceResponseCallback(t1));
					} catch (Exception e) {
						t1.onError(e);
					}
				}

			}));
		}
		return result.toList();
	}
	
	private Collection<ClusterMember> getAllClusterMembers() {
		return this.clusterMembers.values();
	}
	
	private ClusterMember getTargetMember(RoutingKey routingKey) {
		int targetPartition = routingKey.hashCode() % partitionCount();
		ClusterMember target = this.clusterMembers.get(targetPartition);
		if (target == null) {
			throw new ServiceUnavailableException("Failed to find cluster member with id: " + targetPartition);
		}
		return target;
	}

	private FutureCallback<HttpResponse> serviceResponseCallback(
			final Subscriber<? super AstrixServiceInvocationResponse> t1) {
		return new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse response) {
				try {
					t1.onNext(getResponse(response));
					t1.onCompleted();
				} catch (Exception e) {
					t1.onError(e);
				}
			}
			public void failed(final Exception ex) {
				t1.onError(ex);
			}
			public void cancelled() {
				t1.onError(new RuntimeException("Request cancelled"));
			}
		};
	}
	
	private AstrixServiceInvocationResponse getResponse(final HttpResponse response){
		try {
			HttpEntity entity = response.getEntity();
			ObjectInputStream inputStream = new ObjectInputStream(entity.getContent());
			return (AstrixServiceInvocationResponse) inputStream.readObject();
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int partitionCount() {
		return this.clusterSize;
	}
	
	@PostConstruct
	public void init() {
	    httpclient.start();
	}
	
	@PreDestroy
	public void destroy() throws IOException {
		httpclient.close();
	}
	
	private static class ClusterMember {
		private String remoteEndpoint;
		private int clusterInstanceId;
		public ClusterMember(String remoteEndpoint, int clusterInstanceId) {
			this.remoteEndpoint = remoteEndpoint;
			this.clusterInstanceId = clusterInstanceId;
		}
		
		public String getRemoteEndpointUri() {
			return remoteEndpoint;
		}
		
		public int getClusterInstanceId() {
			return clusterInstanceId;
		}
		
		
	}

}
