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
package se.avanzabank.asterix.core;

public interface AsterixObjectSerializer {
	
	<T> T deserialize(Object element, Class<T> type, int version);

	Object serialize(Object element, int version);
	
	int version();

	public static class NoVersioningSupport implements AsterixObjectSerializer {
		
		public static final int NO_VERSIONING = -21;
		
		@Override
		public <T> T deserialize(Object element, Class<T> type, int version) {
			if (type.isPrimitive()) {
				return (T) element;
			}
			return type.cast(element);
		}
		@Override
		public Object serialize(Object element, int version) {
			return element;
		}
		@Override
		public int version() {
			return NO_VERSIONING;
		}
	}
}
