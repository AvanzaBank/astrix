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
package com.avanza.astrix.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyProvider<T> implements Module {

	private final static Logger log = LoggerFactory.getLogger(StrategyProvider.class);
	
	private static final StrategyContextPreparer NO_PREPARER = new StrategyContextPreparer() {
		@Override
		public void prepare(StrategyContext context) {
		}
	};
	private final Class<T> strategyType;
	private final Class<? extends T> strategyImpl;
	private final T strategyInstance;
	private final StrategyContextPreparer strategyContextPreparer;

	private StrategyProvider(Class<T> strategyType, Class<? extends T> strategyImpl, T strategyInstance,
			StrategyContextPreparer strategyContextPreparer) {
		this.strategyType = strategyType;
		this.strategyImpl = strategyImpl;
		this.strategyInstance = strategyInstance;
		this.strategyContextPreparer = strategyContextPreparer;
	}
	
	public static <T> StrategyProvider<T> create(Class<T> strategyType, Class<? extends T> strategyImpl, StrategyContextPreparer strategyContextPreparer) {
		return new StrategyProvider<>(strategyType, strategyImpl, null, strategyContextPreparer);
	}
	
	public static <T> StrategyProvider<T> create(Class<T> strategyType, T strategyInstance) {
		return new StrategyProvider<>(strategyType, null, strategyInstance, NO_PREPARER);
	}
	
	public static <T> StrategyProvider<T> create(Class<T> strategyType, Class<? extends T> strategyImpl) {
		return new StrategyProvider<>(strategyType, strategyImpl, null, NO_PREPARER);
	}

	protected final Class<T> getStrategyType() {
		return this.strategyType;
	}

	protected final Class<? extends T> getStrategyImpl() {
		return this.strategyImpl;
	}

	@Override
	public final String name() {
		if (strategyInstance != null) {
			return this.strategyInstance.getClass().getName();
		}
		return getStrategyImpl().getName();
	}

	protected final void prepare(StrategyContext context) {
		strategyContextPreparer.prepare(context);
	}

	@Override
	public void prepare(final ModuleContext moduleContext) {
		strategyContextPreparer.prepare(new StrategyContext() {
			@Override
			public void importType(Class<?> type) {
				moduleContext.importType(type);
			}

			@Override
			public <E> void bind(Class<E> type, Class<? extends E> providerType) {
				moduleContext.bind(type, providerType);
			}
		});
		if (this.strategyInstance != null) {
			log.debug("Exporting strategy. type={} provider={}", strategyType.getName(), strategyInstance.getClass().getName());
			moduleContext.bind(strategyType, strategyInstance);
		} else if (this.strategyImpl != null) {
			log.debug("Exporting strategy. type={} provider={}", strategyType.getName(), strategyImpl.getName());
			moduleContext.bind(strategyType, strategyImpl);
		}
		moduleContext.export(strategyType);
	}

}
