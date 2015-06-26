package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.registry.ServiceRegistryDiscoveryPluginImpl;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

public class ServiceRegistryDiscoveryModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceDiscoveryMetaFactoryPlugin.class, ServiceRegistryDiscoveryPluginImpl.class);
		
		moduleContext.importType(AstrixPublishedBeans.class);
		
		moduleContext.export(ServiceDiscoveryMetaFactoryPlugin.class);
	}

	@Override
	public String name() {
		return "service-registry-discovery";
	}

}
