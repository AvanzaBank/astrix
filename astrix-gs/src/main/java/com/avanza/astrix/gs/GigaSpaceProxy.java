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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

import org.openspaces.core.GigaSpace;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.BeanFaultTolerance;
import com.avanza.astrix.ft.CheckedCommand;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.gigaspaces.internal.client.cache.SpaceCacheException;
/**
 * This proxy adds fault tolerance to a GigaSpace clustered proxy.
 * 
 * 1. All invocations will be protected using AstrixFaultTolerance.
 * 2. Some exceptions thrown by the space-proxy will be wrapped in ServiceUnavailableException
 *    to ensure circuit-breaker logic is triggered when space is not available.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class GigaSpaceProxy implements InvocationHandler {

	private final GigaSpace gigaSpace;
	private final BeanFaultTolerance faultTolerance;
	private final HystrixCommandSettings hystrixSettings;

	public GigaSpaceProxy(GigaSpace gigaSpace, BeanFaultTolerance faultTolerance, HystrixCommandSettings hystrixSettings) {
		this.gigaSpace = Objects.requireNonNull(gigaSpace);
		this.faultTolerance = Objects.requireNonNull(faultTolerance);
		this.hystrixSettings = Objects.requireNonNull(hystrixSettings);
	}

	public static GigaSpace create(GigaSpace gigaSpace, BeanFaultTolerance faultTolerance, HystrixCommandSettings hystrixSettings) {
		return ReflectionUtil.newProxy(GigaSpace.class, new GigaSpaceProxy(gigaSpace, faultTolerance, hystrixSettings));
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		return faultTolerance.execute(new CheckedCommand<Object>() {
			@Override
			public Object call() throws Throwable {
				try {
					return ReflectionUtil.invokeMethod(method, gigaSpace, args);
				} catch (SpaceCacheException e) {
					throw new ServiceUnavailableException("SpaceCacheNotAvailable", e);
				}
			}
		}, this.hystrixSettings);
	}

}
