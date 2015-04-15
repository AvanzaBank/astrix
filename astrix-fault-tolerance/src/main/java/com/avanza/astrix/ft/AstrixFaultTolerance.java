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

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixFaultTolerance implements AstrixConfigAware {
	
	private DynamicConfig config;
	private final Impl impl;
	
	@AstrixInject
	public AstrixFaultTolerance() {
		this(new HystrixImpl());
	}
	
	AstrixFaultTolerance(Impl impl) {
		this.impl = impl;
	}

	public <T> Observable<T> observe(Observable<T> observable, ObservableCommandSettings settings) {
		if (faultToleranceEnabled(settings)) {
			return impl.observe(observable, settings);
		} else {
			return observable;
		}
	}
	
	public <T> T execute(final Command<T> command, HystrixCommandSettings settings) {
		try {
			return execute((CheckedCommand<T>)command, settings);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			// This is a programming bug, Command<T> cannot throw checked exception
			throw new RuntimeException("Programming bug", e);
		}
	}
	
	public <T> T execute(final CheckedCommand<T> command, HystrixCommandSettings settings) throws Throwable {
		if (faultToleranceEnabled(settings)) {
			return impl.execute(command, settings);
		} else {
			return command.call();
		}
	}

	private <T> boolean faultToleranceEnabled(HystrixCommandKeys keys) {
		DynamicBooleanProperty faultToleranceEnabledForCircuit = config.getBooleanProperty("astrix.faultTolerance." + keys.getCommandKey() + ".enabled", true);
		DynamicBooleanProperty faultToleranceEnabled = AstrixSettings.ENABLE_FAULT_TOLERANCE.getFrom(config);
		boolean enabled = faultToleranceEnabled.get() && faultToleranceEnabledForCircuit.get();
		return enabled;
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	interface Impl {
		<T> Observable<T> observe(Observable<T> observable, ObservableCommandSettings settings);
		<T> T execute(final CheckedCommand<T> command, HystrixCommandSettings settings) throws Throwable;
	}
	
	static class HystrixImpl implements Impl {
		public <T> Observable<T> observe(Observable<T> observable, ObservableCommandSettings settings) {
			return HystrixObservableCommandFacade.observe(observable, settings);
		}
		public <T> T execute(final CheckedCommand<T> command, HystrixCommandSettings settings) throws Throwable {
			return HystrixCommandFacade.execute(command, settings);
		}
	}
	
}
