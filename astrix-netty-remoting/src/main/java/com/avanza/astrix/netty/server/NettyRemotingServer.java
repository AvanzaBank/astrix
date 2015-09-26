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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import com.avanza.astrix.remoting.server.AstrixServiceActivator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class NettyRemotingServer {

	private int port = 12003;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private AtomicBoolean started = new AtomicBoolean(false);
	private AstrixServiceActivator serviceActivator;
	
	public NettyRemotingServer(AstrixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}

	public void verifyStarted() {
		if (this.started.compareAndSet(false, true)) {
			start();
		}
	}
	
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(
                        new ObjectEncoder(),
                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                        new NettyRemotingServerHandler(serviceActivator));
            }
         });

        // Bind and start to accept incoming connections. Binds to all interfaces
        b.bind(port);
    }
    
    @PreDestroy
	public void stop() {
    	if (bossGroup != null) {
    		bossGroup.shutdownGracefully();
    		workerGroup.shutdownGracefully();
    	}
	}

	public int getPort() {
		return this.port;
	}

}