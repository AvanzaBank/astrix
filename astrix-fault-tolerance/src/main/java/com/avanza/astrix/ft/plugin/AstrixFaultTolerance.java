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
package com.avanza.astrix.ft.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.Command;
import com.avanza.astrix.ft.CommandSettings;
import com.avanza.astrix.ft.FaultToleranceSpecification;
import com.avanza.astrix.ft.HystrixCommandFacade;
import com.avanza.astrix.ft.HystrixCommandKeys;
import com.avanza.astrix.ft.HystrixObservableCommandFacade;
import com.avanza.astrix.ft.ObservableCommandSettings;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixFaultTolerance implements AstrixConfigAware {
	
	private AstrixFaultTolerancePlugin faultTolerancePlugin;
	private DynamicConfig config;
	
	
	public AstrixFaultTolerance(AstrixFaultTolerancePlugin faultTolerancePlugin) {
		this.faultTolerancePlugin = faultTolerancePlugin;
	}

	public <T> T addFaultTolerance(FaultToleranceSpecification<T> spec, T provider) {
		DynamicBooleanProperty faultToleranceEnabledForCircuit = config.getBooleanProperty("astrix.faultTolerance." + spec.getApi().getName() + ".enabled", true);
		DynamicBooleanProperty faultToleranceEnabled = config.getBooleanProperty(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		T withFaultTolerance = faultTolerancePlugin.addFaultTolerance(spec, provider);
		return ReflectionUtil.newProxy(spec.getApi(), new FaultToleranceToggle<>(withFaultTolerance, provider, faultToleranceEnabled, faultToleranceEnabledForCircuit));
	}
	
	public <T> Observable<T> observe(Observable<T> observable, ObservableCommandSettings settings) {
		if (faultToleranceEnabled(settings)) {
			return HystrixObservableCommandFacade.observe(observable, settings);
		} else {
			return observable;
		}
	}
	
	public <T> T execute(final Command<T> command, CommandSettings settings) {
		if (faultToleranceEnabled(settings)) {
			return HystrixCommandFacade.execute(command, settings);
		} else {
			return command.call();
		}
	}

	private <T> boolean faultToleranceEnabled(HystrixCommandKeys keys) {
		DynamicBooleanProperty faultToleranceEnabledForCircuit = config.getBooleanProperty("astrix.faultTolerance." + keys.getCommandKey() + ".enabled", true);
		DynamicBooleanProperty faultToleranceEnabled = config.getBooleanProperty(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		boolean enabled = faultToleranceEnabled.get() && faultToleranceEnabledForCircuit.get();
		return enabled;
	}

	private class FaultToleranceToggle<T> implements InvocationHandler {
		private final T faultToleranceProtectedProvider;
		private final T rawProvider;
		private final DynamicBooleanProperty useFaultTolerance;
		private final DynamicBooleanProperty faultToleranceEnabledForCircuit;
		
		public FaultToleranceToggle(T faultToleranceProtectedProvider, T rawProvider, DynamicBooleanProperty useFaultTolerance, DynamicBooleanProperty faultToleranceEnabledForCircuit) {
			this.faultToleranceProtectedProvider = faultToleranceProtectedProvider;
			this.rawProvider = rawProvider;
			this.useFaultTolerance = useFaultTolerance;
			this.faultToleranceEnabledForCircuit = faultToleranceEnabledForCircuit;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				T provider = getProvider();
				return method.invoke(provider, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		private T getProvider() {
			if (useFaultTolerance.get() && faultToleranceEnabledForCircuit.get()) {
				return faultToleranceProtectedProvider;
			}
			return rawProvider;
		}
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
}
