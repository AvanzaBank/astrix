package com.avanza.astrix.context.versioning;

import com.avanza.astrix.beans.service.AstrixVersioningPlugin;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class JacksonVersioningModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(AstrixVersioningPlugin.class, JacksonVersioningPlugin.class);
		
		moduleContext.export(AstrixVersioningPlugin.class);
	}

	@Override
	public String name() {
		return "jackson-versioning";
	}

}
