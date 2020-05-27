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
package com.avanza.astrix.gs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.avanza.astrix.config.DynamicConfig;

/**
 * A bridge between the security manager that GigaSpaces instantiates itself and the application context which provides
 * the dynamic config. Another way to implement dynamic config is having logic for contacting an authentication authority
 * like a database or web server in the security manager.
 */
@Service
public class ContextBridge implements ApplicationContextAware {
	private static final Logger LOG = LoggerFactory.getLogger(ContextBridge.class);
	private static ApplicationContext ctx;

	public static DynamicConfig waitForConfig() {
		long start = System.currentTimeMillis();
		while (ctx == null) {
			if (start + 100 < System.currentTimeMillis()) {
				return null;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				LOG.error("Threw exception on Thread.sleep waiting for config", e);
				return null;
			}
		}
		return ctx.getBean(DynamicConfig.class);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ctx = applicationContext;
	}
}
