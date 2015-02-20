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

import java.lang.reflect.Method;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.Versioned;

public class AstrixPublishedBeanDefinitionMethod {
	
	private Method method;
	
	private AstrixPublishedBeanDefinitionMethod(Method method) {
		this.method = method;
	}

	public boolean isLibrary() {
		return method.isAnnotationPresent(Library.class);
	}

	public AstrixBeanKey<?> getBeanKey() {
		return AstrixBeanKey.create(getBeanType(), getQualifier());
	}

	public String getQualifier() {
		if (method.isAnnotationPresent(AstrixQualifier.class)) {
			return method.getAnnotation(AstrixQualifier.class).value();
		}
		return null;
	}

	public boolean isService() {
		return method.isAnnotationPresent(Service.class);
	}
	
	public String getServiceComponentName() {
		if (!method.getAnnotation(Service.class).value().isEmpty()) {
			return method.getAnnotation(Service.class).value();
		}
		return null;
	}

	public boolean isVersioned() {
		return method.isAnnotationPresent(Versioned.class);
	}

	public static AstrixPublishedBeanDefinitionMethod create(Method astrixBeanDefinition) {
		return new AstrixPublishedBeanDefinitionMethod(astrixBeanDefinition);
	}

	public Class<?> getBeanType() {
		return method.getReturnType();
	}
}