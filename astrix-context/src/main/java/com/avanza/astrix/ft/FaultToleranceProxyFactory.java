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
package com.avanza.astrix.ft;

import java.util.List;


/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class FaultToleranceProxyFactory {
	
	private FaultToleranceProxyProvider faultToleranceProxyProvider;

	public FaultToleranceProxyFactory(List<FaultToleranceProxyProvider> faultToleranceProxyProviders) {
		if (faultToleranceProxyProviders.isEmpty()) {
			this.faultToleranceProxyProvider = new NoFaultToleranceProvider();
		} else {
			this.faultToleranceProxyProvider = faultToleranceProxyProviders.get(0);
		}
	}

	public <T> T addFaultTolerance(final Class<T> api, T provider, HystrixCommandKeys commandKeys) {
		return faultToleranceProxyProvider.addFaultToleranceProxy(api, provider, commandKeys);
	}
	
	private static final class NoFaultToleranceProvider implements FaultToleranceProxyProvider {
		@Override
		public <T> T addFaultToleranceProxy(Class<T> type, T rawProvider, HystrixCommandKeys commandKeys) {
			return rawProvider;
		}
		
	}
}
