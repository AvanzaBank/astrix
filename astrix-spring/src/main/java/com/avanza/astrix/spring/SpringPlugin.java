package com.avanza.astrix.spring;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;

@MetaInfServices(AstrixContextPlugin.class)
public class SpringPlugin implements AstrixContextPlugin {

	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
		astrixContextConfig.registerModule(new SpringModule());
	}

}
