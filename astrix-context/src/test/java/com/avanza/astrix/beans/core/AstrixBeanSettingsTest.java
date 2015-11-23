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
package com.avanza.astrix.beans.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;

public class AstrixBeanSettingsTest {
	
	@Test
	public void qualifiedSettingName() throws Exception {
		IntBeanSetting testSetting = new IntBeanSetting("test.setting", 0);
		assertEquals("astrix.bean." + Ping.class.getName() + "-theQualifier" + ".test.setting", testSetting.nameFor(AstrixBeanKey.create(Ping.class, "theQualifier")));
	}
	
	@Test
	public void unqualifiedSettingName() throws Exception {
		IntBeanSetting testSetting = new IntBeanSetting("test.setting", 0);
		assertEquals("astrix.bean." + Ping.class.getName() + ".test.setting", testSetting.nameFor(AstrixBeanKey.create(Ping.class)));
	}
	
	
	interface Ping {
	}
	

}
