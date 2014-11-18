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
package com.avanza.astrix.versioning.plugin;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AsterixApiDescriptor;
import com.avanza.astrix.context.AsterixVersioningPlugin;
import com.avanza.astrix.core.AsterixObjectSerializer;
import com.avanza.astrix.provider.versioning.AsterixVersioned;

@MetaInfServices(AsterixVersioningPlugin.class)
public class JacksonVersioningPlugin implements AsterixVersioningPlugin {
	@Override
	public AsterixObjectSerializer create(AsterixApiDescriptor descriptor) {
		if (descriptor.isAnnotationPresent(AsterixVersioned.class)) {
			AsterixVersioned versioningInfo = descriptor.getAnnotation(AsterixVersioned.class);
			return new VersionJacksonAsterixObjectSerializer(versioningInfo);
		}
		return new AsterixObjectSerializer.NoVersioningSupport();
	}

}
