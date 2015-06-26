package com.avanza.astrix.remoting.server;

import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;

public interface AstrixServiceActivator {

	AstrixServiceInvocationResponse invokeService(AstrixServiceInvocationRequest invocationRequest);

	void register(Object provider, AstrixObjectSerializer objectSerializer, Class<?> publishedApi);

}
