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

import java.lang.annotation.Annotation;



/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceLookup {
	
	private Impl<?> impl;

	public AstrixServiceLookup(Impl<?> impl) {
		this.impl = impl;
	}

	public static <T extends Annotation> AstrixServiceLookup create(AstrixServiceLookupPlugin<T> serviceLookupPlugin, T lookupInfo) {
		return new AstrixServiceLookup(new Impl<T>(serviceLookupPlugin, lookupInfo));
	}

	public AstrixServiceProperties lookup(Class<?> beanType, String optionalQualifier) {
		return impl.lookup(beanType, optionalQualifier);
	}
	
	private static class Impl<T extends Annotation> {
		private AstrixServiceLookupPlugin<T> serviceLookupPlugin;
		private T lookupInfo;
		
		Impl(AstrixServiceLookupPlugin<T> serviceLookupPlugin, T lookupInfo) {
			this.serviceLookupPlugin = serviceLookupPlugin;
			this.lookupInfo = lookupInfo;
		}

		AstrixServiceProperties lookup(Class<?> beanType, String optionalQualifier) {
			return serviceLookupPlugin.lookup(beanType, optionalQualifier, lookupInfo);
		}
	}
	
}
