package com.avanza.astrix.remoting.server;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixContextConfig;
import com.avanza.astrix.context.AstrixContextPlugin;

@MetaInfServices(AstrixContextPlugin.class)
public class RemotingServerPlugin implements AstrixContextPlugin {

	@Override
	public void register(AstrixContextConfig astrixContextConfig) {
		astrixContextConfig.registerModule(new RemotingServerModule());
	}

}
