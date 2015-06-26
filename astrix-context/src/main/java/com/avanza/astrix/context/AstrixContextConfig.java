package com.avanza.astrix.context;

import com.avanza.astrix.context.module.Module;

public interface AstrixContextConfig {

	void registerModule(Module module);

	<T> void registerStrategy(Class<T> strategyType, T defaultInstance);

	<T> void bindStrategy(Class<T> strategy, Module module);

}
