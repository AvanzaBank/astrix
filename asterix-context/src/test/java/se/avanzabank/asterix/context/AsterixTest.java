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
package se.avanzabank.asterix.context;

import static org.junit.Assert.*;

import org.junit.Test;

import se.avanzabank.asterix.provider.library.AsterixExport;
import se.avanzabank.asterix.provider.library.AsterixLibraryProvider;


public class AsterixTest {
	
	
	@Test
	public void testName() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(TestLibraryDescriptor.class);
		AsterixContext asterixContext = asterixConfigurer.configure();
		
		TestLibraryBean libraryBean = asterixContext.getBean(TestLibraryBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
//	@Test
	public void supportsLibrariesExportedUsingClasses() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(TestLibraryDescriptor.class);
		AsterixContext asterixContext = asterixConfigurer.configure();
		
		TestLibraryBean libraryBean = asterixContext.getBean(TestLibraryBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	
	interface TestLibraryBean {
		String hello(String msg);
	}
	
	static class TestLibraryBeanImpl implements TestLibraryBean {
		public String hello(String msg) {
			return "hello: " + msg;
		}
	}

	@AsterixLibraryProvider
	static class TestLibraryDescriptor {
		
		@AsterixExport
		public TestLibraryBean testLibraryBean() {
			return new TestLibraryBeanImpl();
		}
	}
	
	

}
