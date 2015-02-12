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
package com.avanza.astrix.context;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
class LeasedService<T> {

	private final AstrixServiceLookup serviceLookup;
	private final AstrixServiceBeanInstance<T> instance;
	private volatile AstrixServiceProperties currentProperties;
	private final Lock stateLock = new ReentrantLock();

	public LeasedService(AstrixServiceBeanInstance<T> instance, 
			AstrixServiceProperties currentProperties,
			AstrixServiceLookup serviceLookup) {
		this.instance = instance;
		this.currentProperties = currentProperties;
		this.serviceLookup = serviceLookup;
	}

	public void renew() {
		stateLock.lock();
		try {
			AstrixServiceProperties serviceProperties = serviceLookup.lookup(getBeanKey());
			refreshServiceProperties(serviceProperties);
		} finally {
			stateLock.unlock();
		}
	}
	
	public boolean isBound() {
		return this.instance.isBound();
	}
	
	private void refreshServiceProperties(AstrixServiceProperties serviceProperties) {
		if (serviceHasChanged(serviceProperties)) {
			this.instance.bind(serviceProperties);
			currentProperties = serviceProperties;
		}
	}

	private boolean serviceHasChanged(AstrixServiceProperties serviceProperties) {
		return !Objects.equals(currentProperties, serviceProperties);
	}

	AstrixBeanKey<T> getBeanKey() {
		return this.instance.getBeanKey();
	}

}
