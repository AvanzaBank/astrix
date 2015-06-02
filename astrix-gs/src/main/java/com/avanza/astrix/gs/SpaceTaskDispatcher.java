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
package com.avanza.astrix.gs;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.avanza.astrix.core.util.NamedThreadFactory;
import com.avanza.astrix.remoting.util.GsUtil;
import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.internal.client.spaceproxy.SpaceProxyImpl;
import com.j_spaces.core.IJSpace;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class SpaceTaskDispatcher {
	
	/*
	 * IMPLEMENTATION NOTE:
	 * 
	 * The SpaceTaskDispatcher is required due to a flaw in the design of GigaSpace.execute methods.
	 * In rare conditions that method might block, by default up to as long as 30 seconds if no
	 * resources are available to process the request. In order to ensure a non-blocking programming model
	 * we associate each clustered proxy with a dedicated thread pool to submit task executions, which ensures
	 * that a service invocation will never block, see com.avanza.astrix.gs.remoting.GsRemotingTransport
	 */
	
	private static final Logger log = LoggerFactory.getLogger(SpaceTaskDispatcher.class);
	private final GigaSpace gigaSpace;
	private final ThreadPoolExecutor executorService;

	public SpaceTaskDispatcher(GigaSpace gigaSpace, DynamicConfig config) {
		this.gigaSpace = gigaSpace;
		/* 
		 * TODO 
		 * 	(1) Improve configuration mechanism used to configure thread pool. 
		 */
		String spaceInstanceName = gigaSpace.getName();
		DynamicIntProperty poolSize = config.getIntProperty("astrix.beans.gigaspace." + spaceInstanceName + ".spaceTaskDispatcher.poolsize", 10);
		this.executorService = new ThreadPoolExecutor(poolSize.get(), 
											 poolSize.get(), 
											 0, 
											 TimeUnit.SECONDS,
											 new LinkedBlockingQueue<Runnable>(),
											 new NamedThreadFactory(String.format("SpaceTaskDispatcher[%s]", spaceInstanceName)));
		poolSize.addListener(new DynamicPropertyListener<Integer>() {
			@Override
			public void propertyChanged(Integer newValue) {
				log.info(String.format("Changing pool-size for SpaceTaskDistpatcher. space=%s newSize=%s, oldSize=%s", 
										SpaceTaskDispatcher.this.gigaSpace.getName(), 
										newValue, executorService.getMaximumPoolSize()));
				executorService.setCorePoolSize(newValue);
				executorService.setMaximumPoolSize(newValue);
			}
		});
	}


	public IJSpace getSpace() {
		return gigaSpace.getSpace();
	}
	
	public int partitionCount() {
		IJSpace space = this.gigaSpace.getSpace();
		if (space instanceof SpaceProxyImpl) {
			return SpaceProxyImpl.class.cast(space).getSpaceClusterInfo().getNumberOfPartitions();
		}
		throw new IllegalStateException("Cant decide cluster topology on clustered proxy: " + this.gigaSpace.getName());
	}

	public <T extends Serializable> Observable<T> observe(final Task<T> task, final Object routingKey) {
		return Observable.create(new OnSubscribe<T>() {
			@Override
			public void call(final Subscriber<? super T> t1) {
				executorService.execute(new Runnable() {
					@Override
					public void run() {
						// Submit task on current thread in executorService
						AsyncFuture<T> taskResult = gigaSpace.execute(task, routingKey);
						GsUtil.subscribe(taskResult, t1);
					}
				});
			}
		});
	}

	public <T extends Serializable, R> Observable<R> observe(final DistributedTask<T, R> distributedTask) {
		return Observable.create(new OnSubscribe<R>() {
			@Override
			public void call(final Subscriber<? super R> t1) {
				executorService.execute(new Runnable() {
					@Override
					public void run() {
						// Submit task on current thread in executorService
						AsyncFuture<R> taskResult = gigaSpace.execute(distributedTask);
						GsUtil.subscribe(taskResult, t1);
					}
				});
			}
		});
	}
	
	/**
	 * Destroys the {@link SpaceTaskDispatcher} by shutting down the underlying
	 * {@link ExecutorService}. <p>
	 */
	public void destroy() {
		this.executorService.shutdown();
	}

	public String getSpaceName() {
		return gigaSpace.getName();
	}
}