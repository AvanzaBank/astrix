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
package se.avanzabank.asterix.remoting.client;

import java.util.Objects;

import javax.annotation.Resource;

import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.Task;

import se.avanzabank.asterix.remoting.server.AsterixServiceActivator;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@AutowireTask
public class AsterixServiceInvocationTask implements Task<AsterixServiceInvocationResponse> {

	private static final long serialVersionUID = 1L;

	@Resource
	private transient AsterixServiceActivator asterixServiceActivator;
	private final AsterixServiceInvocationRequest invocationRequest;
	
	public AsterixServiceInvocationTask(AsterixServiceInvocationRequest invocationRequest) {
		this.invocationRequest = Objects.requireNonNull(invocationRequest);
	}

	@Override
	public AsterixServiceInvocationResponse execute() throws Exception {
		return asterixServiceActivator.invokeService(invocationRequest);
	}

}
