package com.avanza.astrix.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/*
 * Copyright 2014 Avanza Bank AB
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
/**
 * 
 * @author Elias Lindholm
 *
 */
public class HttpRemotingEndpoint extends HttpServlet {
	
	/*
	 * This is only a draft for remoting over http
	 */
	
	private final AstrixServiceActivator serviceActivator;
	
	public HttpRemotingEndpoint(AstrixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ObjectInputStream input = new ObjectInputStream(req.getInputStream());
			AstrixServiceInvocationRequest invocationRequest = (AstrixServiceInvocationRequest) input.readObject();
			AstrixServiceInvocationResponse invocationResponse = serviceActivator.invokeService(invocationRequest);
			new ObjectOutputStream(resp.getOutputStream()).writeObject(invocationResponse);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

}
