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

import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.spring.AstrixFrameworkBean;
import com.avanza.astrix.test.util.AstrixTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AstrixApplicationTest {
	
	// TODO: Convert to unit test using AstrixConfigurer?
	
	
	private AnnotationConfigApplicationContext appContext;
	
	@AfterEach
	void after() {
		AstrixTestUtil.closeQuiet(appContext);
	}

	@Test
	void throwsExceptionIfExportedRemoteServiceDoesNotPointToAnApiProvider() {
		appContext = new AnnotationConfigApplicationContext();
		appContext.register(MyAppConfig.class);
		assertThrows(RuntimeException.class, () -> appContext.refresh());
	}
	
	public interface MyService {
	}
	
	@AstrixApplication(
		exportsRemoteServicesFor = MyService.class, // Note: Not an ApiProvider
		defaultServiceComponent = AstrixServiceComponentNames.DIRECT
	)
	public static class MyAppDescriptor {
	}
	
	
	@Configuration
	public static class MyAppConfig {
		@Bean
		public static AstrixFrameworkBean astrix() {
			AstrixFrameworkBean astrix = new AstrixFrameworkBean();
			astrix.setApplicationDescriptor(MyAppDescriptor.class);
			return astrix;
		}
	}

}
