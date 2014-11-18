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

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixBeanStates implements AstrixEventListener<AstrixBeanStateChangedEvent> {
	
	private final ConcurrentMap<AstrixBeanKey, ListenableEnumReference<AstrixBeanState>> beanStateByBeanKey = new ConcurrentHashMap<>();
	
	public void waitForBeanToBeBound(AstrixBeanKey beanKey, long timeoutMillis) throws InterruptedException {
		ListenableEnumReference<AstrixBeanState> beanState = getBeanState(beanKey);
		boolean bound = beanState.waitForValue(AstrixBeanState.BOUND, timeoutMillis);
		if (!bound) {
			throw new RuntimeException("Bean with key=" + beanKey + " was not bound before timeout");
		}
	}

	private ListenableEnumReference<AstrixBeanState> getBeanState(AstrixBeanKey beanKey) {
		ListenableEnumReference<AstrixBeanState> beanState = beanStateByBeanKey.get(beanKey);
		if (beanState != null) {
			return beanState;
		}
		beanStateByBeanKey.putIfAbsent(beanKey, new ListenableEnumReference<>(AstrixBeanState.class));
		return beanStateByBeanKey.get(beanKey);
	}

	@Override
	public void onEvent(AstrixBeanStateChangedEvent event) {
		getBeanState(event.getBeanKey()).set(event.getNewBeanState());
	}
	
	static class ListenableEnumReference<T extends Enum<T>> {
		
		private volatile T value;
		private final EnumMap<T, Object> monitorByEnumValue;
				
		public ListenableEnumReference(Class<T> type) {
			monitorByEnumValue = new EnumMap<>(type);
			for (T value : type.getEnumConstants()) {
				monitorByEnumValue.put(value, new Object());
			}
		}
		
		public void set(T value) {
			this.value = value;
			Object enumValueMonitor = getMonitor(value);
			synchronized (enumValueMonitor) {
				enumValueMonitor.notifyAll();
			}
		}
		
		private Object getMonitor(T value) {
			return monitorByEnumValue.get(value);
		}

		public boolean waitForValue(T value, long timeoutMillis) throws InterruptedException {
			if (this.value == value) {
				return true;
			}
			Object enumValueMonitor = getMonitor(value);
			synchronized (enumValueMonitor) {
				enumValueMonitor.wait(timeoutMillis);
				return this.value == value;
			}
		}
	}
	
	

}
