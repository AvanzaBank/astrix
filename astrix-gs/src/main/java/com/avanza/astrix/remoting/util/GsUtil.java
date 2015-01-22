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
package com.avanza.astrix.remoting.util;

import java.util.ArrayList;
import java.util.List;

import org.openspaces.remoting.SpaceRemotingResult;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceInvocationException;

/**
 * @author joasah
 */
public class GsUtil {
	
	public static <T> List<AstrixRemoteResult<T>> convertToAstrixRemoteResults(SpaceRemotingResult<T>[] results) {
		List<AstrixRemoteResult<T>> converted = new ArrayList<AstrixRemoteResult<T>>(results.length);
		for (SpaceRemotingResult<T> result : results) {
			if (result.getException() != null) {
				converted.add(AstrixRemoteResult.<T>failure(toRuntimeException(result.getException())));
			} else {
				converted.add(AstrixRemoteResult.successful(result.getResult()));
			}
		}
		return converted;
	}
	
	private static ServiceInvocationException toRuntimeException(Throwable exception) {
		if (exception instanceof RuntimeException) {
			return (ServiceInvocationException) exception;
		} else {
			return new RemoteServiceInvocationException("Remote service threw exception: " + exception.getMessage(), exception.getClass().getName(), ServiceInvocationException.UNDEFINED_CORRELATION_ID);
		}
	}
	
}
