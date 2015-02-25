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

import java.util.Objects;

import com.avanza.astrix.context.IsolationStrategy;

/**
 * @author Kristoffer Erlandsson (krierl)
 */
public class FaultToleranceSpecification<T> {
	
	private final Class<T> api;
	private final String group;
	private final IsolationStrategy isolationStrategy;
	
	private FaultToleranceSpecification(Class<T> api, String group, IsolationStrategy isolationStrategy) {
		this.api = api;
		this.group = group;
		this.isolationStrategy = isolationStrategy;
	}
	
	public Class<T> getApi() {
		return api;
	}

	public String getGroup() {
		return group;
	}

	public IsolationStrategy getIsolationStrategy() {
		return isolationStrategy;
	}

	public static class Builder<T> {
		private String group;
		private IsolationStrategy isolationStrategy;
		private Class<T> api;
		
		public Builder(Class<T> api) {
			Objects.requireNonNull(api);
			this.api = api;
		}
		
		public Builder<T> isolationStrategy(IsolationStrategy isolationStrategy) {
			this.isolationStrategy = isolationStrategy;
			return this;
		}
		
		public Builder<T> group(String group) {
			this.group = group;
			return this;
		}
		
		public FaultToleranceSpecification<T> build() {
			Objects.requireNonNull(group);
			Objects.requireNonNull(isolationStrategy);
			return new FaultToleranceSpecification<T>(api, group, isolationStrategy);
		}
		
	}
	
	public static <T> Builder<T> builder(Class<T> api) {
		return new Builder<T>(api);
	}
	
	
}
