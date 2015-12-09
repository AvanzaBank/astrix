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
package com.avanza.astrix.context.mbeans;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;

public class PlatformMBeanServer implements MBeanServerFacade {

	private static final AtomicInteger astrixContextCount = new AtomicInteger(0);
	
	private final Logger logger = LoggerFactory.getLogger(PlatformMBeanServer.class);
	private final String domain;
	
	public PlatformMBeanServer(AstrixConfig astrixConfig) {
		int astrixContextId = astrixContextCount.incrementAndGet();
		if (astrixContextId != 1) {
			this.domain = "com.avanza.astrix.context." + astrixContextId;
		} else {
			this.domain = "com.avanza.astrix.context";
		}
	}

	@Override
	public void registerMBean(Object mbean, String folder, String name) {
		try {
			ObjectName objectName = getObjectName(folder, name);
			ManagementFactory.getPlatformMBeanServer().registerMBean(mbean, objectName);
		} catch (Exception e) {
			logger.warn(String.format("Failed to export mbean: type=%s domain=%s subdomain=%s name=%s", mbean.getClass().getName(), domain, folder, name.toString()), e);
		}
	}
	
	

	private ObjectName getObjectName(String folder, String name) {
		try {
			return new ObjectName(domain + ":00=" + folder + ",name=" + name);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}
	
}
