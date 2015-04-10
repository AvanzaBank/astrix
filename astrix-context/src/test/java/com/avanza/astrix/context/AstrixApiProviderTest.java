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
package com.avanza.astrix.context;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.AstrixDynamicQualifier;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
import com.avanza.astrix.provider.versioning.Versioned;


public class AstrixApiProviderTest {
	
	@Test
	public void apiWithOneLibrary() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test
	public void librariesShouldNotBeStateful() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class);
		assertEquals("Expected non-stateful astrix bean without a proxy.", PingLibImpl.class, ping.getClass());
	}
	
	@Test
	public void librariesCanBeQualifiedToDistinguishProviders() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingAndReversePingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class, "ping");
		PingLib reversePing = context.getBean(PingLib.class, "reverse-ping");
		assertEquals("hello", ping.ping("hello"));
		assertEquals("olleh", reversePing.ping("hello"));
	}
	
	@Test
	public void servicesCanBeQualifiedToDistinguishProviders() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set("pingServiceUri", AstrixDirectComponent.registerAndGetUri(PingService.class, new PingServiceImpl()));
		astrixConfigurer.set("reversePingServiceUri", AstrixDirectComponent.registerAndGetUri(PingService.class, new ReversePingServiceImpl()));
		astrixConfigurer.registerApiProvider(PingAndReversePingServiceProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService ping = context.getBean(PingService.class, "ping");
		PingService reversePing = context.getBean(PingService.class, "reverse-ping");
		assertEquals("hello", ping.ping("hello"));
		assertEquals("olleh", reversePing.ping("hello"));
	}
	
	@Test
	public void apiWithOneLibraryAndOneService() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, new PingServiceImpl());
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingServiceAndLibraryProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib pingLib = context.getBean(PingLib.class);
		assertEquals("foo", pingLib.ping("foo"));
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	
	@Test
	public void versionedApi() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ServiceVersioningContext.versionedService(1, DummyObjectSerializerConfigurer.class));
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(AstrixVersioningPlugin.class, new JavaSerializationVersioningPlugin());
		astrixConfigurer.registerApiProvider(VersionedPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		astrixConfigurer.enableVersioning(true);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	@Test
	public void apiWithVersionedAnNonVersionedService() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ServiceVersioningContext.versionedService(1, DummyObjectSerializerConfigurer.class));
		String internalPingServiceUri = AstrixDirectComponent.registerAndGetUri(InternalPingService.class, 
																		new InternalPingServiceImpl(), 
																		ServiceVersioningContext.nonVersioned());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(AstrixVersioningPlugin.class, new JavaSerializationVersioningPlugin());
		astrixConfigurer.registerApiProvider(PublicAndInternalPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		astrixConfigurer.set("internalPingServiceUri", internalPingServiceUri);
		astrixConfigurer.enableVersioning(true);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		InternalPingService internalPingService = context.getBean(InternalPingService.class);
		assertEquals("foo", pingService.ping("foo"));
		assertEquals("bar", internalPingService.ping("bar"));
	}
	
	@Test
	public void supportsDynamicQualifiedServices() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(DynamicPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService1 = context.getBean(PingService.class, "foo");
		PingService pingService2 = context.getBean(PingService.class, "bar");
		
		assertEquals("foo", pingService1.ping("foo"));
		assertEquals("foo", pingService2.ping("foo"));
		
	}
	
	public interface PingLib {
		String ping(String msg);
	}
	
	public interface PingService {
		String ping(String msg);
	}

	public interface InternalPingService {
		String ping(String msg);
	}
	
	public static class PingLibImpl implements PingLib {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class ReversePingLibImpl implements PingLib {
		@Override
		public String ping(String msg) {
			return new StringBuilder(msg).reverse().toString();
		}
	}
	
	public static class PingServiceImpl implements PingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class ReversePingServiceImpl implements PingService {
		@Override
		public String ping(String msg) {
			return new StringBuilder(msg).reverse().toString();
		}
	}
	
	public static class InternalPingServiceImpl implements InternalPingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	@AstrixApiProvider
	public static class PingLibraryProvider {
		@Library
		public PingLib myLib() {
			return new PingLibImpl();
		}
	}
	
	@AstrixApiProvider
	public static class PingAndReversePingLibraryProvider {

		@AstrixQualifier("ping")
		@Library
		public PingLib ping() {
			return new PingLibImpl();
		}

		@AstrixQualifier("reverse-ping")
		@Library
		public PingLib reversePing() {
			return new ReversePingLibImpl();
		}
	}
	@AstrixApiProvider
	public interface PingAndReversePingServiceProvider {

		@AstrixConfigLookup("pingServiceUri")
		@AstrixQualifier("ping")
		@Service
		PingService pingLib();

		@AstrixConfigLookup("reversePingServiceUri")
		@AstrixQualifier("reverse-ping")
		@Service
		PingService reversePingLib();
	}

	@AstrixApiProvider
	public static class PingServiceAndLibraryProvider {
		@Library
		public PingLib myLib() {
			return new PingLibImpl();
		}
		
		@AstrixConfigLookup("pingServiceUri")
		@Service
		public PingService pingService() {
			return null;
		}
	}
	
	@AstrixObjectSerializerConfig(
		version = 1,
		objectSerializerConfigurer = DummyObjectSerializerConfigurer.class
	)
	@AstrixApiProvider
	public interface VersionedPingServiceProvider {
		
		@Versioned
		@AstrixConfigLookup("pingServiceUri")
		@Service
		PingService pingService();
	}

	public interface InternalPingServiceApi {
		@AstrixConfigLookup("internalPingServiceUri")
		@Service
		InternalPingService internalPingService();
	}
	
	@AstrixObjectSerializerConfig(
		version = 1,
		objectSerializerConfigurer = DummyObjectSerializerConfigurer.class
	)
	@AstrixApiProvider
	public interface PublicAndInternalPingServiceProvider {
		
		@Versioned
		@AstrixConfigLookup("pingServiceUri")
		@Service
		PingService pingService();
		
		@AstrixConfigLookup("internalPingServiceUri")
		@Service
		InternalPingService internalPingService();
	}
	
	public static class DummyObjectSerializerConfigurer implements AstrixObjectSerializerConfigurer {
	}
	
	@AstrixApiProvider
	public interface DynamicPingServiceProvider {

		@AstrixConfigLookup("pingServiceUri")
		@AstrixDynamicQualifier
		@Service
		PingService pingService();
	}

}
