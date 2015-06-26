package com.avanza.astrix.spring;

import org.springframework.beans.factory.config.BeanPostProcessor;

import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;
import com.avanza.astrix.serviceunit.ServiceExporter;

public class SpringModule implements NamedModule {

	@Override
	public void prepare(ModuleContext moduleContext) {
		moduleContext.bind(AstrixSpringContext.class, AstrixSpringContextImpl.class);
		moduleContext.bind(BeanPostProcessor.class, AstrixBeanPostProcessor.class);
		
		moduleContext.importType(ServiceExporter.class);
		
		moduleContext.export(AstrixSpringContext.class);
		moduleContext.export(BeanPostProcessor.class); // TODO: this is only exported in order to read it from AstrixFrameworkBean. Can we avoid exporting it?
	}

	@Override
	public String name() {
		return getClass().getPackage().getName();
	}

}
