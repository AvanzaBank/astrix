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
package com.avanza.astrix.versioning.core;

import java.lang.reflect.Type;

public interface AstrixObjectSerializer {
	
	/**
	 * Deserializes a serialized object. 
	 * 
	 * @param element
	 * @param type
	 * @param version - the version of the serialized form of the object
	 * @return
	 */
	<T> T deserialize(Object element, Type type, int version);

	
	/**
	 * Serializes a given object to the serialized form of a given version.
	 * 
	 * @param element
	 * @param version - the version of the serialized data format to serialize the object to.
	 * @return
	 */
	Object serialize(Object element, int version);
	
	int version();

	public static class NoVersioningSupport implements AstrixObjectSerializer {
		
		public static final int NO_VERSIONING = -21;
		
		@Override
		public <T> T deserialize(Object element, Type type, int version) {
			return (T) element;
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
