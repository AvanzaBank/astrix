package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.context.ConfigServiceDiscoveryPluginImpl;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class ConfigDiscoveryModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceDiscoveryMetaFactoryPlugin.class, ConfigServiceDiscoveryPluginImpl.class);
		
		moduleContext.importType(ServiceComponentRegistry.class);
		moduleContext.importType(AstrixConfig.class);
		
		moduleContext.export(ServiceDiscoveryMetaFactoryPlugin.class);
	}

	@Override
	public String name() {
		return "config-discovery";
	}

}
