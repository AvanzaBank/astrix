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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.factory.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviders;

public class AstrixConfigurerTest {
	
	@Test
	public void passesBeanSettingsToConfiguration() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return Collections.emptyList();
			}
		});
		IntBeanSetting intSetting = new IntBeanSetting("intSetting", 1);
		BooleanBeanSetting aBooleanSetting = new BooleanBeanSetting("booleanSetting", true);
		LongBeanSetting longSetting = new LongBeanSetting("longSetting", 2);
		
		configurer.set(aBooleanSetting, AstrixBeanKey.create(Ping.class), false);
		configurer.set(intSetting, AstrixBeanKey.create(Ping.class), 21);
		configurer.set(longSetting, AstrixBeanKey.create(Ping.class), 19);
		
		AstrixContext astrixContext = configurer.configure();
		
		assertEquals(21, intSetting.getFor(AstrixBeanKey.create(Ping.class), astrixContext.getConfig()).get());
		assertFalse(aBooleanSetting.getFor(AstrixBeanKey.create(Ping.class), astrixContext.getConfig()).get());
		assertEquals(19, longSetting.getFor(AstrixBeanKey.create(Ping.class), astrixContext.getConfig()).get());
	}
	
	public interface Ping {
	}

}
