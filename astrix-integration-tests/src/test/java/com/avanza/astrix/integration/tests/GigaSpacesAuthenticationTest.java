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
package com.avanza.astrix.integration.tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;
import org.openspaces.core.space.CannotFindSpaceException;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.DefaultTraceProvider;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.gs.ClusteredProxyCacheImpl;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.gs.security.DefaultGsSecurityProvider;
import com.avanza.astrix.gs.security.GsSecurityProvider;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;
import com.avanza.gs.test.SecurityManagerForTests;
import com.gigaspaces.security.directory.CredentialsProvider;
import com.gigaspaces.security.directory.DefaultCredentialsProvider;
import com.gigaspaces.security.directory.User;

public class GigaSpacesAuthenticationTest {

	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	private static final MapConfigSource settings = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
	}};

	// PU with authentication
	@ClassRule
	public static final RunningPu LUNCH_PU = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
			.contextProperty("configSourceId", GlobalConfigSourceRegistry.register(settings))
			.beanProperties("space", new Properties() {{
				// This is equivalent to configuring the pu in pu.xml like so:
				// <os-core:space id="space" url="/./${spaceName}" mirror="false" versioned="true">
				//   <os-core:security secured="true" />
				// </os-core:space>
				setProperty("secured", "true");
			}})
			.withAuthentication()
			.configure();

	// PU without authentication
	@ClassRule
	public static final RunningPu LUNCH_GRADER_PU = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
			.contextProperty("configSourceId", GlobalConfigSourceRegistry.register(settings))
			.configure();

	private final AstrixTraceProvider traceProvider = new DefaultTraceProvider();
	private final GsSecurityProvider mockGsSecurityProvider = new DefaultGsSecurityProvider() {
		@Override
		public CredentialsProvider getGsClientCredentialsProvider(String spaceName) {
			return new DefaultCredentialsProvider(clientCredentials);
		}
	};
	private final ServiceProperties lunchPuServiceProperties = new GsBinder().createProperties(LUNCH_PU.getClusteredGigaSpace());
	private final ServiceProperties lunchGraderPuServiceProperties = new GsBinder().createProperties(LUNCH_GRADER_PU.getClusteredGigaSpace());
	private final ClusteredProxyCacheImpl proxyCache = new ClusteredProxyCacheImpl(traceProvider, mockGsSecurityProvider);

	/**
	 * This are the client-side credentials that will be used when connecting to
	 * {@link #LUNCH_PU}. The server-side will validate the credentials using
	 * {@link SecurityManagerForTests}, and we have no good way of affecting the
	 * server-side validator from these tests.
	 */
	private final User clientCredentials = new User(SecurityManagerForTests.TEST_USER, SecurityManagerForTests.TEST_PASS);

	@Test
	public void shouldAllowConnectingToGigaSpacesUsingClientCredentials() {
		// Act
		proxyCache.getProxy(lunchPuServiceProperties);

		// Assert that no exceptions were thrown
	}

	@Test
	public void shouldDisallowConnectingToGigaSpacesUsingClientCredentialsWithWrongPassword() {
		// Arrange
		System.getProperties().remove("com.gs.security.credentials-provider.class");
		clientCredentials.setPassword("incorrect");

		// Act
		try {
			proxyCache.getProxy(lunchPuServiceProperties);

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (CannotFindSpaceException e) {
			assertThat(e.toString(), containsString("Failed to find space"));
			assertThat(e.getRootCause().toString(), containsString("Incorrect auth password"));
		}
	}

	@Test
	public void shouldDisallowConnectingToSecuredGigaSpacesWithoutUsingClientCredentials() {
		// Arrange
		lunchPuServiceProperties.setProperty("isSecured", "false");

		// Act
		try {
			proxyCache.getProxy(lunchPuServiceProperties).get().clear(null);

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (Exception e) {
			assertThat(e.toString(), containsString("No credentials were provided"));
		}
	}

	@Test
	public void shouldAllowConnectingToNonSecureGigaSpacesWithoutClientCredentials() {
		// Act
		proxyCache.getProxy(lunchGraderPuServiceProperties);

		// Assert that no exceptions were thrown
	}

	@Test
	public void shouldDisallowConnectingToNonSecureGigaSpacesUsingClientCredentials() {
		// Arrange
		lunchGraderPuServiceProperties.setProperty("isSecured", "true");

		// Act
		try {
			proxyCache.getProxy(lunchGraderPuServiceProperties);

			// Assert
			fail("Expected an exception to be thrown here, but no exception was seen.");
		} catch (CannotFindSpaceException e) {
			assertThat(e.toString(), containsString("Failed to find space"));
			assertThat(e.getRootCause().toString(), containsString("Can't provide security credentials to a non-secured space"));
		}
	}
}
