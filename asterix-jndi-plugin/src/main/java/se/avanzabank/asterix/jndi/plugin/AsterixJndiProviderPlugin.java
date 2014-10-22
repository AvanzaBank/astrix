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
package se.avanzabank.asterix.jndi.plugin;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixApiProviderPlugin;
import se.avanzabank.asterix.context.AsterixFactoryBeanPlugin;
import se.avanzabank.asterix.provider.core.AsterixJndiApi;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixJndiProviderPlugin implements AsterixApiProviderPlugin {

	@Override
	public List<AsterixFactoryBeanPlugin<?>> createFactoryBeans(AsterixApiDescriptor descriptor) {
		AsterixJndiApi jndiApi = descriptor.getAnnotation(AsterixJndiApi.class);
		String entryName = jndiApi.entryName();
		Class<?> beanType = jndiApi.exportedApi();
		AsterixJndiLookupFactoryBean<?> factory = new AsterixJndiLookupFactoryBean<>(entryName, descriptor, beanType);
		return Arrays.<AsterixFactoryBeanPlugin<?>>asList(factory);
	}

	@Override
	public List<Class<?>> getProvidedBeans(AsterixApiDescriptor descriptor) {
		return Arrays.<Class<?>>asList(descriptor.getAnnotation(AsterixJndiApi.class).exportedApi());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixJndiApi.class;
	}

	@Override
	public boolean isLibraryProvider() {
		return false;
	}

}
