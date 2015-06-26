package com.avanza.astrix.gs.localview;

import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.spring.AstrixSpringContext;

public class GsLocalViewModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, GsLocalViewComponent.class);
		
		moduleContext.importType(AstrixSpringContext.class);
		moduleContext.importType(BeanFaultToleranceFactory.class);
		moduleContext.importType(ServiceComponentRegistry.class);
		
		moduleContext.export(ServiceComponent.class);
		moduleContext.export(ClusteredProxyCache.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
