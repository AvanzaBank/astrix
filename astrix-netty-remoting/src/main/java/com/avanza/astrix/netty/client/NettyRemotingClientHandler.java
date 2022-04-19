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
package com.avanza.astrix.netty.client;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import rx.Observable;
import rx.Subscriber;

public class NettyRemotingClientHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(NettyRemotingClientHandler.class);

	public static final String NETTY_RESPONSE_SUBSCRIBER_ID = "netty.responseSubscriberId";
	private volatile ChannelHandlerContext ctx;
	
	private ConcurrentMap<String, Subscriber<? super AstrixServiceInvocationResponse>> subscriberBySubscriberId = new ConcurrentHashMap<>(); 

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
//         Send the first message if this handler is a client-side handler.
//        ctx.writeAndFlush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	AstrixServiceInvocationResponse response = (AstrixServiceInvocationResponse) msg;
    	String invocationId = response.getHeader(NETTY_RESPONSE_SUBSCRIBER_ID);
    	Subscriber<? super AstrixServiceInvocationResponse> subscriber = getSubscriber(invocationId);
    	if (subscriber == null) {
    		return;
    	}
    	subscriber.onNext(response);
    	subscriber.onCompleted();
        // Echo back the received object to the server.
//        ctx.write(msg);
    }

    private Subscriber<? super AstrixServiceInvocationResponse> getSubscriber(String invocationId) {
    	Subscriber<? super AstrixServiceInvocationResponse> subscriber = this.subscriberBySubscriberId.remove(invocationId);
		return subscriber;
	}

	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.trace("Exception caught", cause);
        ctx.close();
    }

	public Observable<AstrixServiceInvocationResponse> sendInvocationRequest(AstrixServiceInvocationRequest request) {
		return Observable.unsafeCreate((subscriber) -> {
			String invocationId = UUID.randomUUID().toString();
			this.subscriberBySubscriberId.put(invocationId, subscriber);
			request.setHeader(NETTY_RESPONSE_SUBSCRIBER_ID, invocationId);
			ctx.writeAndFlush(request);
		});
	}
}