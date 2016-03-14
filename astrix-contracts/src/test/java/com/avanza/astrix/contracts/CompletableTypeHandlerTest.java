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
package com.avanza.astrix.contracts;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.avanza.astrix.beans.rx.CompletableTypeHandler;
import rx.Completable;

public class CompletableTypeHandlerTest extends ReactiveTypeHandlerContract<Completable> {
	@Override
	protected ReactiveTypeHandlerPlugin<Completable> newReactiveTypeHandler() {
		return new CompletableTypeHandler();
	}

	@Override
	protected String valueToTest() {
		return null;
	}
}
