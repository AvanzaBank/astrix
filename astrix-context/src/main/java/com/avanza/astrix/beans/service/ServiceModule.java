package com.avanza.astrix.beans.service;

import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class ServiceModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponentRegistry.class, ServiceComponents.class);
		moduleContext.bind(ObjectSerializerFactory.class, ObjectSerializerFactoryImpl.class);
		
		moduleContext.importType(AstrixVersioningPlugin.class);
		moduleContext.importType(ServiceComponent.class);
		
		moduleContext.export(ServiceComponentRegistry.class);
		moduleContext.export(ObjectSerializerFactory.class);
	}

	@Override
	public String name() {
		return "service";
	}

}
