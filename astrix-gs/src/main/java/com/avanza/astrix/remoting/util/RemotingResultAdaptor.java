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
package com.avanza.astrix.remoting.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.openspaces.remoting.SpaceRemotingResult;

import com.avanza.astrix.core.AstrixRemoteResult;

public interface RemotingResultAdaptor<T> {
	public T getResult() throws Throwable;

	static class GigaSpaceRemotingResultWrap<T> implements RemotingResultAdaptor<T> {
		private final SpaceRemotingResult<T> source;

		public GigaSpaceRemotingResultWrap(SpaceRemotingResult<T> source) {
			this.source = Objects.requireNonNull(source);
		}

		@Override
		public T getResult() throws Throwable {
			if (source.getException() != null) {
				throw source.getException();
			} else {
				return source.getResult();
			}
		}

		public static <T> Collection<RemotingResultAdaptor<T>> wrap(SpaceRemotingResult<T>[] results) {
			ArrayList<RemotingResultAdaptor<T>> wrapped = new ArrayList<>(results.length);
			for (SpaceRemotingResult<T> result : results) {
				wrapped.add(new GigaSpaceRemotingResultWrap<T>(result));
			}
			return wrapped;
		}
	}

	static class AstrixRemotingResultWrap<T> implements RemotingResultAdaptor<T> {
		private final AstrixRemoteResult<T> source;

		public AstrixRemotingResultWrap(AstrixRemoteResult<T> source) {
			this.source = Objects.requireNonNull(source);
		}

		@Override
		public T getResult() {
			return source.getResult();
		}

		public static <T> Collection<RemotingResultAdaptor<T>> wrap(Collection<AstrixRemoteResult<T>> results) {
			ArrayList<RemotingResultAdaptor<T>> wrapped = new ArrayList<>(results.size());
			for (AstrixRemoteResult<T> result : results) {
				wrapped.add(new AstrixRemotingResultWrap<T>(result));
			}
			return wrapped;
		}
	}
}
