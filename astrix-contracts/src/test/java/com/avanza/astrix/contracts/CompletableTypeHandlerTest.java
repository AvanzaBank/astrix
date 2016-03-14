package com.avanza.astrix.contracts;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.avanza.astrix.beans.rx.CompletableTypeHandler;
import rx.Completable;

/**
 * Created by Daniel Bergholm
 */
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
