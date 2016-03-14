package com.avanza.astrix.contracts;

import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.avanza.astrix.beans.rx.SingleTypeHandler;
import rx.Single;

/**
 * Created by Daniel Bergholm
 */
public class SingleTypeHandlerTest extends ReactiveTypeHandlerContract<Single<Object>> {
	@Override
	protected ReactiveTypeHandlerPlugin<Single<Object>> newReactiveTypeHandler() {
		return new SingleTypeHandler();
	}
}
