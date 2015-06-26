package com.avanza.astrix.gs.remoting;

import com.avanza.astrix.beans.service.ObjectSerializerFactory;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;

public class GsRemotingModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(ServiceComponent.class, GsRemotingComponent.class);
		
		moduleContext.importType(ObjectSerializerFactory.class);
		moduleContext.importType(AstrixSpringContext.class);
		moduleContext.importType(BeanFaultToleranceFactory.class);
		moduleContext.importType(ClusteredProxyCache.class);
		moduleContext.importType(AstrixServiceActivator.class);
		
		moduleContext.export(ServiceComponent.class);
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
