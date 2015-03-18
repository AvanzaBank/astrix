/*
 * Copyright 2014-2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.remoting.client;

import com.avanza.astrix.core.AstrixRemoteResultReducer;
import com.avanza.astrix.core.util.ReflectionUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class RemoteServiceMethod {
	
	private String signature;
	private Router router;
	private Class<? extends AstrixRemoteResultReducer> reducer;
	
	public RemoteServiceMethod(String signature, Router router) {
		this.signature = signature;
		this.router = router;
	}

	public RemoteServiceMethod(String signature,
			@SuppressWarnings("rawtypes") Class<? extends AstrixRemoteResultReducer> reducer) {
		this.signature = signature;
		this.reducer = reducer;
	}

	public String getSignature() {
		return signature;
	}
	
	public RoutingKey getRoutingKey(Object... args) throws Exception {
		return this.router.getRoutingKey(args);
	}
	
	public boolean isBroadcast() {
		return router == null;
	}
	
	public AstrixRemoteResultReducer<?, ?> newReducer() {
		return ReflectionUtil.newInstance(this.reducer);
	}
	
}