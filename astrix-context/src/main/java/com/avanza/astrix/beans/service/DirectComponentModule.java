package com.avanza.astrix.beans.service;

import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class DirectComponentModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, DirectComponent.class);
		
		moduleContext.importType(AstrixVersioningPlugin.class);
		
		moduleContext.export(ServiceComponent.class);
	}

	@Override
	public String name() {
		return "direct-component";
	}

}
