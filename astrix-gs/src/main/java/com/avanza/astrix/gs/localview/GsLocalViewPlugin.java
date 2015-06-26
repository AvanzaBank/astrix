package com.avanza.astrix.gs.localview;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;

@MetaInfServices(AstrixContextPlugin.class)
public class GsLocalViewPlugin implements AstrixContextPlugin {

	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
		astrixContextConfig.registerModule(new GsLocalViewModule());
	}

}
