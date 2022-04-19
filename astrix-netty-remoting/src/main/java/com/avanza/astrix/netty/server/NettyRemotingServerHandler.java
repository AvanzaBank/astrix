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
package com.avanza.astrix.netty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.netty.client.NettyRemotingClientHandler;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyRemotingServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(NettyRemotingClientHandler.class);

	private AstrixServiceActivator serviceActivator;
	
    public NettyRemotingServerHandler(AstrixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}
    
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	AstrixServiceInvocationRequest request = (AstrixServiceInvocationRequest) msg;
		AstrixServiceInvocationResponse response = serviceActivator.invokeService(request);
    	response.setHeader(NettyRemotingClientHandler.NETTY_RESPONSE_SUBSCRIBER_ID, request.getHeader(NettyRemotingClientHandler.NETTY_RESPONSE_SUBSCRIBER_ID));
        ctx.write(response);
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
}