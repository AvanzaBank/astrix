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

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.gs.GsBinder;
import com.j_spaces.core.IJSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openspaces.core.GigaSpace;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GsBinderIntegrationTest {


	private final MapConfigSource config = new MapConfigSource();
	private AnnotationConfigApplicationContext applicationContext;
	
	@AfterEach
	void closeContext() {
		applicationContext.close();
	}

	@Test
	void usesQualifierToIdentifyWhatEmbeddedSpaceToUse() {
		applicationContext = new AnnotationConfigApplicationContext(AppWithTwoEmbeddedSpaces.class);
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		config.set(AstrixSettings.GIGA_SPACE_BEAN_NAME, "gigaSpace");
		GigaSpace gigaSpace = gsBinder.getEmbeddedSpace(applicationContext);
		
		GigaSpace expected = applicationContext.getBean("gigaSpace", GigaSpace.class);
		assertSame(expected, gigaSpace,"Expected embedded space to be defined by GIGA_SPACE_BEAN_NAME property");
	}
	
	@Test
	void usesEmbeddedSpaceOverClusteredProxies() {
		applicationContext = new AnnotationConfigApplicationContext(AppWithClusteredProxyAndEmbeddedSpace.class);
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		GigaSpace gigaSpace = gsBinder.getEmbeddedSpace(applicationContext);
		
		GigaSpace expected = applicationContext.getBean("gigaSpace", GigaSpace.class);
		assertTrue(expected.getSpace().isEmbedded());
		assertSame(expected, gigaSpace, "Expected embedded space to be returned");
	}
	
	@Configuration
	static class AppWithTwoEmbeddedSpaces {
		@Bean
		public GigaSpace gigaSpace() {
			IJSpace space = mock(IJSpace.class);
			GigaSpace gs = mock(GigaSpace.class);
			when(gs.getSpace()).thenReturn(space);
			when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
		@Bean
		public GigaSpace otherEmbeddedGigaSpace() {
			IJSpace space = mock(IJSpace.class);
			GigaSpace gs = mock(GigaSpace.class);
			when(gs.getSpace()).thenReturn(space);
			when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
	}
	
	@Configuration
	static class AppWithClusteredProxyAndEmbeddedSpace {
		@Bean
		public GigaSpace gigaSpace() {
			IJSpace space = mock(IJSpace.class);
			GigaSpace gs = mock(GigaSpace.class);
			when(gs.getSpace()).thenReturn(space);
			when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
		@Bean
		public GigaSpace clusteredGigaSpace() {
			IJSpace space = mock(IJSpace.class);
			GigaSpace gs = mock(GigaSpace.class);
			when(gs.getSpace()).thenReturn(space);
			when(space.isEmbedded()).thenReturn(false);
			return gs;
		}
	}
}
