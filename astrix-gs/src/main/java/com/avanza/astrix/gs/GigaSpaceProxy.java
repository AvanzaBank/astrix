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
import com.gigaspaces.internal.client.cache.SpaceCacheException;
/**
 * 1. Some exceptions thrown by the space-proxy will be wrapped in ServiceUnavailableException
 *    to ensure circuit-breaker logic is triggered when space is not available.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class GigaSpaceProxy implements InvocationHandler {

	private final GigaSpace gigaSpace;

	public GigaSpaceProxy(GigaSpace gigaSpace) {
		this.gigaSpace = Objects.requireNonNull(gigaSpace);
	}

	public static GigaSpace create(GigaSpace gigaSpace) {
		return ReflectionUtil.newProxy(GigaSpace.class, new GigaSpaceProxy(gigaSpace));
	}

	@Override
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
		try {
			return ReflectionUtil.invokeMethod(method, gigaSpace, args);
		} catch (SpaceCacheException e) {
			throw new ServiceUnavailableException("SpaceCacheNotAvailable", e);
		}
	}

}
