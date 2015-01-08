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
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.context.AstrixFaultTolerancePlugin;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.avanza.astrix.context.FaultToleranceSpecification;
import com.avanza.astrix.ft.HystrixAdapter;


@MetaInfServices(value = AstrixFaultTolerancePlugin.class)
public class HystrixFaultTolerancePlugin implements AstrixFaultTolerancePlugin, AstrixSettingsAware {
	
	private AstrixSettingsReader settings;
	
	@Override
	public <T> T addFaultTolerance(FaultToleranceSpecification<T> spec) {
		Objects.requireNonNull(spec);
		DynamicBooleanProperty useFaultTolerance = settings.getDynamicBooleanProperty("astrix.faultTolerance." + spec.getGroup() + "." + spec.getApi().getName() + ".enabled", true);
		T faultToleranceProtectedProvider = HystrixAdapter.create(spec);
		FaultToleranceToggle<T> fsToggle = new FaultToleranceToggle<T>(faultToleranceProtectedProvider, spec.getProvider(), useFaultTolerance);
		return spec.getApi().cast(Proxy.newProxyInstance(spec.getApi().getClassLoader(), new Class[]{spec.getApi()}, fsToggle));
	}
	
	private class FaultToleranceToggle<T> implements InvocationHandler {
		private final T faultToleranceProtectedProvider;
		private final T rawProvider;
		private final DynamicBooleanProperty useFaultTolerance;
		
		public FaultToleranceToggle(T faultToleranceProtectedProvider, T rawProvider, DynamicBooleanProperty useFaultTolerance) {
			this.faultToleranceProtectedProvider = faultToleranceProtectedProvider;
			this.rawProvider = rawProvider;
			this.useFaultTolerance = useFaultTolerance;
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
			if (useFaultTolerance.get()) {
				return faultToleranceProtectedProvider;
			}
			return rawProvider;
		}
	}

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}
	
	

}
