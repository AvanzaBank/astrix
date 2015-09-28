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

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.config.IntSetting;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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
	
	public static final IntSetting NETTY_SERVER_BIND_PORT = IntSetting.create("astrix.netty.server.bindport", 12003);
	private static final Logger log = LoggerFactory.getLogger(NettyRemotingServer.class);

	private int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private AtomicBoolean started = new AtomicBoolean(false);
	private AstrixServiceActivator serviceActivator;
	
	public NettyRemotingServer(AstrixServiceActivator serviceActivator, AstrixConfig config) {
		this.serviceActivator = serviceActivator;
		this.port = config.get(NETTY_SERVER_BIND_PORT).get();
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
         .option(ChannelOption.SO_REUSEADDR, false)
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
        // TODO: Allow specifying a bind port range. Attempt to bind to each port in range and use first successfully bound port
        ChannelFuture channel = b.bind(port);
        try {
			if (channel.await(2, TimeUnit.SECONDS)) {
				if (channel.isSuccess()) {
					port = InetSocketAddress.class.cast(channel.channel().localAddress()).getPort();
					log.info("NettyRemotingServer started listening on port={}", port);
					return;
				}
			}
		} catch (InterruptedException e) {
		}
        throw new IllegalStateException("Failed to start netty remoting server. Can't bind to port: " + port);
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