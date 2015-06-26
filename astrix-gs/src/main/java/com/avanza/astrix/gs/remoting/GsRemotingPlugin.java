package com.avanza.astrix.gs.remoting;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;

@MetaInfServices(AstrixContextPlugin.class)
public class GsRemotingPlugin implements AstrixContextPlugin {

	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
		astrixContextConfig.registerModule(new GsRemotingModule());
	}

}
