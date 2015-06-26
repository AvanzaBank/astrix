package com.avanza.astrix.remoting.server;

import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class RemotingServerModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(AstrixServiceActivator.class, AstrixServiceActivatorImpl.class);
		
		moduleContext.export(AstrixServiceActivator.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
