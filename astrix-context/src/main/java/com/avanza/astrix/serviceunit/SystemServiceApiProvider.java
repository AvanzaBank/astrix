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
package com.avanza.astrix.serviceunit;

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixDynamicQualifier;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.Versioned;

@AstrixObjectSerializerConfig(
	version = 1,
	objectSerializerConfigurer = ServiceAdministratorVersioningConfigurer.class
)
@AstrixApiProvider
public interface SystemServiceApiProvider {
	
	/*
	 * Servern ska styra vilken qualifier som tjänsten registreras under.
	 * 
	 * @AstrixDynamicQualifier ska innebära att denna provider kan binda
	 * alla bönor av typen ServiceAdministrator, oavsett vilken qualifier som efterfrågas.
	 */
	
	//@GenericService?
	//@Qualifier("*")?
	@AstrixDynamicQualifier
	@Versioned
	@Service
	ServiceAdministrator serviceAdministrator();

}
