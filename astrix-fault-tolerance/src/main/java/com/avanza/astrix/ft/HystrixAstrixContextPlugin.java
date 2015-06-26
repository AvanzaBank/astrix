package com.avanza.astrix.ft;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.NamedModule;

@MetaInfServices(AstrixContextPlugin.class)
public class HystrixAstrixContextPlugin implements AstrixContextPlugin {
	
	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
//		astrixContextConfig.registerModule(new HystrixModule());
		
		// Exposes strategy: HystrixCommandNamingStrategy
		astrixContextConfig.registerStrategy(HystrixCommandNamingStrategy.class, new DefaultHystrixCommandNamingStrategy());
		
		astrixContextConfig.bindStrategy(BeanFaultToleranceProxyStrategy.class, new HystrixModule());
	}
	
	private static class HystrixModule implements NamedModule {
		@Override
		public void prepare(ModuleContext moduleContext) {
			moduleContext.bind(BeanFaultToleranceProvider.class, HystrixBeanFaultToleranceProvider.class);
			moduleContext.bind(BeanFaultToleranceProxyStrategy.class, HystrixFaultToleranceProxyProvider.class);
			moduleContext.bind(BeanFaultToleranceFactory.class, BeanFaultToleranceFactoryImpl.class);
			
			moduleContext.importType(HystrixCommandNamingStrategy.class); // strategy
			moduleContext.importType(BeanConfigurations.class);
			
			moduleContext.export(BeanFaultToleranceProvider.class);
			moduleContext.export(BeanFaultToleranceFactory.class);
			
			// TODO: how do we override a strategy???
			moduleContext.export(BeanFaultToleranceProxyStrategy.class); // provide a strategy implementation
									 
		}

		@Override
		public String name() {
			return "hystrix-fault-tolerance";
		}
		
	}
	
}
