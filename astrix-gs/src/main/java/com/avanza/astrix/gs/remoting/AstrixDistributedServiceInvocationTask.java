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
package com.avanza.astrix.gs.remoting;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;

import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.gigaspaces.async.AsyncResult;
/**
 * Carries an AstrixServiceInvocationRequest from client to server and performs
 * the invocation of the {@link AstrixServiceActivator} by using the possibility
 * to autowire spring-beans on the server side. <p>
 * 
 * @author Elias Lindholm
 *
 */
@AutowireTask
public class AstrixDistributedServiceInvocationTask implements DistributedTask<AstrixServiceInvocationResponse, List<AsyncResult<AstrixServiceInvocationResponse>>> {

	private static final long serialVersionUID = 1L;
	@Resource
	private transient AstrixSpringContext astrixSpringContext;
	private final AstrixServiceInvocationRequest request;
	
	public AstrixDistributedServiceInvocationTask(AstrixServiceInvocationRequest request) {
		this.request = Objects.requireNonNull(request);
	}

	@Override
	public AstrixServiceInvocationResponse execute() throws Exception {
		AstrixServiceActivator serviceActivator = astrixSpringContext.getAstrixContext().getInstance(AstrixServiceActivator.class);
		return serviceActivator.invokeService(request);
	}
	
	@Override
	public List<AsyncResult<AstrixServiceInvocationResponse>> reduce(List<AsyncResult<AstrixServiceInvocationResponse>> results) throws Exception {
		return results;
	}


}
