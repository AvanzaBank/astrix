package com.avanza.astrix.gs;

import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.spring.AstrixSpringContext;

public class GsModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, GsComponent.class);
		moduleContext.bind(ClusteredProxyCache.class, ClusteredProxyCacheImpl.class);
		
		moduleContext.importType(AstrixSpringContext.class);
		moduleContext.importType(BeanFaultToleranceFactory.class);
		
		moduleContext.export(ServiceComponent.class);
		moduleContext.export(ClusteredProxyCache.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
