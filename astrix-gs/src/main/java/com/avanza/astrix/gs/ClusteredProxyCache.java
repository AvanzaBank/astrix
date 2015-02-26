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
package com.avanza.astrix.gs;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.KeyLock;
import com.avanza.astrix.beans.factory.ObjectCache;
import com.avanza.astrix.beans.factory.ObjectCache.ObjectFactory;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.j_spaces.core.IJSpace;
/**
 * Manages lifecycle for each clustered-proxy created by Astrix.
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class ClusteredProxyCache {

	
	private static final Logger log = LoggerFactory.getLogger(ClusteredProxyCache.class);
	private final ObjectCache objectCache = new ObjectCache();
	private final KeyLock<String> proxyByUrlLock = new KeyLock<>();
	
	/**
	 * Retreives a given proxy from the cache and creates the proxy if it does not exits.
	 * 
	 * Every time a proxy is retreived from the cache the proxyConsumerCount will be incremented. The
	 * proxy must be returned to the cache by invoking GigaSpaceInstance.release. When all instance
	 * for a proxy agains a given space is release, then the proxy will be destroyed and all associated resources
	 * released. 
	 * 
	 * @param serviceProperties
	 * @return
	 */
	public GigaSpaceInstance getProxy(final AstrixServiceProperties serviceProperties) {
		final String spaceUrl = serviceProperties.getProperty(GsBinder.SPACE_URL_PROPERTY);
		proxyByUrlLock.lock(spaceUrl);
		try {
			GigaSpaceInstance spaceInstance = objectCache.getInstance(spaceUrl, new ObjectFactory<GigaSpaceInstance>() {
				@Override
				public GigaSpaceInstance create() throws Exception {
					log.info("Creating clustered proxy against: " + spaceUrl);
					return new GigaSpaceInstance(spaceUrl);
				}
			});
			spaceInstance.incConsumerCount();
			return spaceInstance;
		} finally {
			proxyByUrlLock.unlock(spaceUrl);
		}
	}
	
	@PreDestroy
	public void destroy() {
		this.objectCache.destroy();
	}
	
	public class GigaSpaceInstance {
		
		private final GigaSpace proxy;
		private final AtomicInteger proxyConsumerCount = new AtomicInteger(0);
		private final String spaceUrl;
		private final UrlSpaceConfigurer urlSpaceConfigurer;
		
		public GigaSpaceInstance(String spaceUrl) {
			this.spaceUrl = spaceUrl;
			this.urlSpaceConfigurer = new UrlSpaceConfigurer(spaceUrl);
			IJSpace space = urlSpaceConfigurer.create();
			this.proxy = new GigaSpaceConfigurer(space).create();
		}

		public void incConsumerCount() {
			this.proxyConsumerCount.incrementAndGet();
		}

		public GigaSpace get() {
			return proxy;
		}
		
		public void release() {
			proxyByUrlLock.lock(spaceUrl);
			try {
				int consumerCount = this.proxyConsumerCount.decrementAndGet();
				if (consumerCount == 0) {
					// Destroy this instance in cache. That in turn will invoke @PreDestroy annotated methods
					objectCache.destroyInCache(spaceUrl);
				}
			} finally {
				proxyByUrlLock.unlock(spaceUrl);
			}
		}
		
		@PreDestroy
		public void destroy() throws Exception {
			log.info("Destroying clustered proxy against: " + spaceUrl);
			urlSpaceConfigurer.destroy();
		}
	}

}
