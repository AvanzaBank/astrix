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
package se.avanzabank.asterix.provider.context;

import java.util.List;

import se.avanzabank.asterix.context.AsterixApiDescriptor;

public interface AsterixServiceRegistryServerComponent {
	
	// TODO: Find suitable name for this abstraction. This is ther server-side correspondence to AsterixServiceRegistryComponent
	
	List<Class<? extends ServiceRegistryExporter>> getRequiredExporterClasses();

	boolean isActivatedBy(AsterixApiDescriptor descriptor);
	
}
