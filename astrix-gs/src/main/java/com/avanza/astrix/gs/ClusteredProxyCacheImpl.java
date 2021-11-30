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

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.async.ContextPropagation;
import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.DefaultTraceProvider;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.gs.security.DefaultGsSecurityProvider;
import com.avanza.astrix.gs.security.GsSecurityProvider;
import com.avanza.astrix.modules.AstrixInject;
import com.avanza.astrix.modules.KeyLock;
import com.avanza.astrix.modules.ObjectCache;
import com.avanza.astrix.modules.ObjectCache.ObjectFactory;
import com.gigaspaces.security.directory.CredentialsProvider;
import com.j_spaces.core.IJSpace;
/**
 * Manages lifecycle for each clustered-proxy created by Astrix.
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class ClusteredProxyCacheImpl implements AstrixConfigAware, ClusteredProxyCache {

	private static final Logger log = LoggerFactory.getLogger(ClusteredProxyCacheImpl.class);
	private final ObjectCache objectCache = new ObjectCache();
	private final KeyLock<String> proxyByUrlLock = new KeyLock<>();
	private final ContextPropagation contextPropagation;
	private final GsSecurityProvider gsSecurityProvider;
	private final GigaspaceMetricsExporter metricsExporter;
	private DynamicConfig config;

	/**
	 * @deprecated please use {@link #ClusteredProxyCacheImpl(AstrixTraceProvider, GsSecurityProvider, GigaspaceMetricsExporter)}
	 */
	@Deprecated
	public ClusteredProxyCacheImpl() {
		this(new DefaultTraceProvider(), new DefaultGsSecurityProvider(), GigaspaceMetricsExporter.noExporter());
	}

	@AstrixInject
	public ClusteredProxyCacheImpl(
			AstrixTraceProvider astrixTraceProvider,
			GsSecurityProvider gsSecurityProvider,
			GigaspaceMetricsExporter metricsExporter) {
		this.contextPropagation = ContextPropagation.create(astrixTraceProvider.getContextPropagators());
		this.gsSecurityProvider = requireNonNull(gsSecurityProvider);
		this.metricsExporter = requireNonNull(metricsExporter);
	}

	/**
	 * Retrieves a given proxy from the cache and creates the proxy if it does not exits.
	 * 
	 * Every time a proxy is retrieved from the cache the proxyConsumerCount will be incremented. The
	 * proxy must be returned to the cache by invoking GigaSpaceInstance.release. When all instance
	 * for a proxy against a given space is release, then the proxy will be destroyed and all associated resources
	 * are released.
	 */
	@Override
	public GigaSpaceInstance getProxy(final ServiceProperties serviceProperties) {
		final String spaceUrl = GsBinder.getSpaceUrl(serviceProperties);
		proxyByUrlLock.lock(spaceUrl);
		try {
			GigaSpaceInstance spaceInstance = objectCache.getInstance(spaceUrl, new ObjectFactory<GigaSpaceInstance>() {
				@Override
				public GigaSpaceInstance create() {
					log.info("Creating clustered proxy against: {}", spaceUrl);
					GigaSpaceInstance gigaSpaceInstance = new GigaSpaceInstance(spaceUrl, config, serviceProperties);
					metricsExporter.exportGigaspaceMetrics();
					return gigaSpaceInstance;
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
		@GuardedBy("spaceTaskDispatcherStateLock")
		private volatile SpaceTaskDispatcher spaceTaskDispatcher;
		private final Lock spaceTaskDispatcherStateLock = new ReentrantLock();
		private final DynamicConfig config;

		public GigaSpaceInstance(String spaceUrl, DynamicConfig dynamicConfig, ServiceProperties serviceProperties) {
			this.spaceUrl = spaceUrl;
			this.config = dynamicConfig;
			this.urlSpaceConfigurer = new UrlSpaceConfigurer(spaceUrl);
			if (GsBinder.isAuthenticationRequired(serviceProperties)) {
				final String spaceName = GsBinder.getSpaceName(serviceProperties);
				log.debug("Connecting to space {} as a secured space.", spaceName);
				urlSpaceConfigurer.credentialsProvider(getCredentialsProvider(spaceName));
			}
			IJSpace space = urlSpaceConfigurer.create();
			this.proxy = new GigaSpaceConfigurer(space).create();
		}

		public void incConsumerCount() {
			this.proxyConsumerCount.incrementAndGet();
		}

		public GigaSpace get() {
			return proxy;
		}
		
		public SpaceTaskDispatcher getSpaceTaskDispatcher() {
			spaceTaskDispatcherStateLock.lock();
			try {
				if (spaceTaskDispatcher == null) {
					this.spaceTaskDispatcher = new SpaceTaskDispatcher(proxy, config, contextPropagation);
				}
				return spaceTaskDispatcher;
			} finally {
				spaceTaskDispatcherStateLock.unlock();
			}
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
			log.info("Destroying clustered proxy against: {}", spaceUrl);
			if (this.spaceTaskDispatcher != null) {
				this.spaceTaskDispatcher.destroy();
			}
			this.urlSpaceConfigurer.close();
		}
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}

	private CredentialsProvider getCredentialsProvider(String spaceName) {
		Object credentialsProvider = System.getProperties().get("com.gs.security.credentials-provider.class");
		if (credentialsProvider instanceof CredentialsProvider) {
			return (CredentialsProvider) credentialsProvider;
		}
		return gsSecurityProvider.getGsClientCredentialsProvider(spaceName);
	}
}
