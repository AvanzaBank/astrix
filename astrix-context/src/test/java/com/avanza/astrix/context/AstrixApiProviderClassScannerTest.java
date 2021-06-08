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
package com.avanza.astrix.context;

import com.avanza.astrix.beans.publish.ApiProviderClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;


class AstrixApiProviderClassScannerTest {
	
	@Test
	void scansDefinedPackagesForDefinedAnnotations() {
		List<ApiProviderClass> apiDescriptors = new AstrixApiProviderClassScanner(singletonList(DummyDescriptor.class), "com.avanza.astrix.context").getAll().collect(toList());
		assertEquals(2, apiDescriptors.size());
		assertThat(apiDescriptors, hasItem(equalTo(ApiProviderClass.create(DescriptorA.class))));
		assertThat(apiDescriptors, hasItem(equalTo(ApiProviderClass.create(DescriptorB.class))));
	}
	
	@Test
	void doesNotFinedDescriptorsOutsideDefinedPackage() {
		List<ApiProviderClass> apiDescriptors = new AstrixApiProviderClassScanner(singletonList(DummyDescriptor.class), "com.avanza.astrix.context.foo.bar").getAll().collect(toList());
		assertEquals(0, apiDescriptors.size());
	}

	public @interface DummyDescriptor {
	}
	
	
	@DummyDescriptor
	public static class DescriptorA {
	}
	
	@DummyDescriptor
	public static class DescriptorB {
	}

}
