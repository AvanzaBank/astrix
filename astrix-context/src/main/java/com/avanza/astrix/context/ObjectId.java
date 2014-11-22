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

import java.util.Objects;

public abstract class ObjectId {
	public static ObjectId astrixBean(AstrixBeanKey key) {
		return new AstrixBeanId(key);
	}
	public static ObjectId internalClass(Class<?> type) {
		return new InternalClassId(type);
	}
	
	public abstract boolean isAstrixBean();
	public abstract boolean isInternalClass();
	public abstract Class<?> getType();
	public abstract String toString();

	@Override
	public int hashCode() {
		return Objects.hash(toString());
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return toString().equals(obj.toString());
	}
	
	public static class AstrixBeanId extends ObjectId {

		private AstrixBeanKey key;

		public AstrixBeanId(AstrixBeanKey key) {
			this.key = key;
		}
		
		public AstrixBeanKey getKey() {
			return key;
		}
		
		@Override
		public boolean isAstrixBean() {
			return true;
		}
		
		@Override
		public boolean isInternalClass() {
			return false;
		}
		
		@Override
		public Class<?> getType() {
			return this.key.getBeanType();
		}
		
		@Override
		public String toString() {
			return key.toString();
		}
	}
	
	public static class InternalClassId extends ObjectId {

		private Class<?> type;

		public InternalClassId(Class<?> type) {
			super();
			this.type = type;
		}

		@Override
		public boolean isAstrixBean() {
			return false;
		}
		
		@Override
		public boolean isInternalClass() {
			return true;
		}
		
		@Override
		public Class<?> getType() {
			return type;
		}
		
		@Override
		public String toString() {
			return type.getName().toString();
		}
	}

}