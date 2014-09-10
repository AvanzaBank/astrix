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
package se.avanzabank.asterix.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CachingAsterixFactoryBean<T> implements AsterixFactoryBean<T>, AsterixDecorator {
	
	private static final String nullKey = "-no-qualifier";
	
	private final AsterixFactoryBean<T> target;
	private final ConcurrentMap<String, T> cacheByQualifier = new ConcurrentHashMap<>();
	private final Lock beanCreationLock = new ReentrantLock();
	
	public CachingAsterixFactoryBean(AsterixFactoryBean<T> target) {
		this.target = target;
	}

	@Override
	public T create(String optionalQualifier) {
		T cachedBean = cacheByQualifier.get(qualifier(optionalQualifier));
		if (cachedBean != null) {
			return cachedBean;
		}
		// TODO: How to avoid dead-locks in case of circular bean creation? Can that happen?
		try {
			beanCreationLock.lock();
			return doCreateBean(optionalQualifier);
		} finally {
			beanCreationLock.unlock();
		}
	}

	private T doCreateBean(String optionalQualifier) {
		T result = cacheByQualifier.get(qualifier(optionalQualifier));
		if (result != null) {
			// Another thread created bean
			return result;
		}
		result = target.create(optionalQualifier);
		cacheByQualifier.put(qualifier(optionalQualifier), result);
		return result;
	}

	private String qualifier(String optionalQualifier) {
		return optionalQualifier != null ? optionalQualifier : nullKey;
	}

	@Override
	public Class<T> getBeanType() {
		return target.getBeanType();
	}

	@Override
	public Object getTarget() {
		return target;
	}

}
