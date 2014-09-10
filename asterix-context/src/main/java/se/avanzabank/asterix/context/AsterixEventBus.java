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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;


public class AsterixEventBus {
	
	// TODO: use guava event-bus?
	
	private final BlockingQueue<Object> pendingEvents = new LinkedBlockingQueue<>();
	private final List<SubscribedListener<?>> subscribedListeners = new CopyOnWriteArrayList<>();
	
	public AsterixEventBus() {
		new EventDispatcher().start(); // TODO: manage lifecycle of EventDispatcher-thread 
	}
	
	public void fireEvent(Object event) {
		pendingEvents.add(event);
	}
	
	public <T> void addEventListener(Class<T> eventType, AsterixEventListener<T> eventListener) {
		subscribedListeners.add(new SubscribedListener<>(eventType, eventListener));
	}
	
	public <T> void removeEventListener(AsterixEventListener<T> eventListener) {
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
		private AsterixEventListener<T> listener;
		
		SubscribedListener(Class<T> eventType, AsterixEventListener<T> listener) {
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
