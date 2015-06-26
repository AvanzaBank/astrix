package com.avanza.astrix.serviceunit;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class ServiceUnitModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceExporter.class, ServiceExporterImpl.class);
		moduleContext.bind(ServiceAdministrator.class, ServiceAdministratorImpl.class);
		moduleContext.bind(ServiceProviderPlugin.class, GenericServiceProviderPlugin.class); // TODO: Should ServiceProviderPlugin be a plugin?
		
		moduleContext.importType(ServiceComponentRegistry.class);
		moduleContext.importType(AstrixConfig.class);
		
		moduleContext.export(ServiceAdministrator.class);
		moduleContext.export(ServiceExporter.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
