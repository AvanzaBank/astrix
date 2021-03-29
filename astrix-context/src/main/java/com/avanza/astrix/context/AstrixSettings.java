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

import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.StringSetting;

/**
 * 
 * @author Elias Lindholm (elilin)
 * @deprecated - Moved to {@link com.avanza.astrix.beans.core.AstrixSettings}
 *
 */
@Deprecated
public class AstrixSettings {
	
	
	/**
	 * @deprecated - Replaced by {@link com.avanza.astrix.beans.core.AstrixSettings#BEAN_BIND_ATTEMPT_INTERVAL}
	 */
	@Deprecated
	public static final LongSetting BEAN_BIND_ATTEMPT_INTERVAL = com.avanza.astrix.beans.core.AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL;

	/**
	 * @deprecated - Replaced by {@link com.avanza.astrix.beans.core.AstrixSettings#SERVICE_REGISTRY_URI}
	 */
	@Deprecated
	public static final StringSetting ASTRIX_SERVICE_REGISTRY_URI = com.avanza.astrix.beans.core.AstrixSettings.SERVICE_REGISTRY_URI;

}
