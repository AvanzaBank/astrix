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

import java.util.concurrent.TimeUnit;

import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import rx.Observable;

public final class NettyRemotingClient {

	private final EventLoopGroup group = new NioEventLoopGroup();
	private final NettyRemotingClientHandler nettyRemotingClientHandler = new NettyRemotingClientHandler();

    public void connect(String host, int port) {
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
				p.addLast(
                        new ObjectEncoder(),
                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                        nettyRemotingClientHandler);
            }
         });

        // Start the connection attempt.
        ChannelFuture channel = b.connect(host, port);
        try {
			if (channel.await(1, TimeUnit.SECONDS)) {
				if (channel.isSuccess()) {
					return;
				}
			}
		} catch (InterruptedException e) {
		}
        destroy();
        throw new IllegalArgumentException(String.format("Failed to connect to remoting server: %s:%d", host, port));
    }
    
    public Observable<AstrixServiceInvocationResponse> invokeService(AstrixServiceInvocationRequest request) {
    	return nettyRemotingClientHandler.sendInvocationRequest(request);
    }
    
    public void destroy() {
    	group.shutdownGracefully();
    }
    		
    		
}