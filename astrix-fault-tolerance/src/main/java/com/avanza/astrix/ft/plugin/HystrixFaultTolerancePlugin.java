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
package com.avanza.astrix.ft.plugin;

import java.util.Objects;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixFaultTolerancePlugin;
import com.avanza.astrix.ft.HystrixAdapter;


@MetaInfServices(value = AstrixFaultTolerancePlugin.class)
public class HystrixFaultTolerancePlugin implements AstrixFaultTolerancePlugin {
	
	@Override
	public <T> T addFaultTolerance(Class<T> api, T provider, String group) {
		Objects.requireNonNull(api);
		Objects.requireNonNull(provider);
		Objects.requireNonNull(group);
		return HystrixAdapter.create(api, provider, group);
	}

}
