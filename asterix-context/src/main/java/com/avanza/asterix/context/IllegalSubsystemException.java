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
package com.avanza.asterix.context;

public class IllegalSubsystemException extends RuntimeException {

	private String currentSubsystem;
	private String beanProviderSubsystem;

	public IllegalSubsystemException(String currentSubsystem, AsterixFactoryBean<?> factoryBean) {
		super(String.format("Its not allowed to inoke beanType=%s in subsystem=%s, currentSubsystem=%s", factoryBean.getBeanType().getName(), factoryBean.getSubsystem(), currentSubsystem));
		this.currentSubsystem = currentSubsystem;
	}
	
	public IllegalSubsystemException(String currentSubsystem, String beanProviderSubsystem, Class<?> beanType) {
		super(String.format("Its not allowed to inoke beanType=%s in providerSubsystem=%s, currentSubsystem=%s", beanType.getName(), beanProviderSubsystem, currentSubsystem));
		this.currentSubsystem = currentSubsystem;
		this.beanProviderSubsystem = beanProviderSubsystem;
	}
	
	public String getCurrentSubsystem() {
		return currentSubsystem;
	}
	
	public String getBeanProviderSubsystem() {
		return beanProviderSubsystem;
	}
}
