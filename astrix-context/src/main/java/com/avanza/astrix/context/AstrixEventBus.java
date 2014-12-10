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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixEventBus {
	
	private final BlockingQueue<Object> pendingEvents = new LinkedBlockingQueue<>();
	private final List<SubscribedListener<?>> subscribedListeners = new CopyOnWriteArrayList<>();
	private final EventDispatcher eventDispatcher = new EventDispatcher();
	
	public AstrixEventBus() {
	}
	
	@PostConstruct
	public void start() {
		this.eventDispatcher.start();
	}
	
	@PreDestroy
	public void destroy() {
		this.eventDispatcher.interrupt();
	}
	
	public void fireEvent(Object event) {
		pendingEvents.add(event);
	}
	
	public <T> void addEventListener(Class<T> eventType, AstrixEventListener<T> eventListener) {
		subscribedListeners.add(new SubscribedListener<>(eventType, eventListener));
	}
	
	public <T> void removeEventListener(AstrixEventListener<T> eventListener) {
		Iterator<SubscribedListener<?>> it = subscribedListeners.iterator();
		while (it.hasNext()) {
			SubscribedListener<?> listener = it.next();
			if (listener.listener == eventListener) {
				it.remove();
			}
		}
	}
	
	private static class SubscribedListener<T> {
		
		private Class<T> eventType;
		private AstrixEventListener<T> listener;
		
		SubscribedListener(Class<T> eventType, AstrixEventListener<T> listener) {
			this.eventType = eventType;
			this.listener = listener;
		}
		
		void dispatch(Object event) {
			if (!eventType.isAssignableFrom(event.getClass())) {
				return;
			}
			listener.onEvent(eventType.cast(event));
		}
		
	}
	
	private class EventDispatcher extends Thread {
		
		public EventDispatcher() {
			super("Astrix-EventBus.Dispatcher");
			setDaemon(true);
		}
		
		@Override
		public void run() {
			while (!interrupted()) {
				try {
					Object nextEvent = pendingEvents.take();
					for (SubscribedListener<?> listener : subscribedListeners) {
						listener.dispatch(nextEvent);
					}
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}
		
	}
	
}
