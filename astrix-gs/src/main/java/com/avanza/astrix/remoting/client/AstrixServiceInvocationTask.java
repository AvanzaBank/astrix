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

import java.util.Objects;

import javax.annotation.Resource;

import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.Task;

import com.avanza.astrix.remoting.server.AstrixServiceActivator;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@AutowireTask
public class AstrixServiceInvocationTask implements Task<AstrixServiceInvocationResponse> {

	private static final long serialVersionUID = 1L;

	@Resource
	private transient AstrixServiceActivator AstrixServiceActivator;
	private final AstrixServiceInvocationRequest invocationRequest;
	
	public AstrixServiceInvocationTask(AstrixServiceInvocationRequest invocationRequest) {
		this.invocationRequest = Objects.requireNonNull(invocationRequest);
	}

	@Override
	public AstrixServiceInvocationResponse execute() throws Exception {
		return AstrixServiceActivator.invokeService(invocationRequest);
	}

}
