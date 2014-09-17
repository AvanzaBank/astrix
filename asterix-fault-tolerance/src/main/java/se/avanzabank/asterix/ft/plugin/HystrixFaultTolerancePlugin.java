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
package se.avanzabank.asterix.ft.plugin;

import java.util.Objects;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixEventLoggerPlugin;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.ft.HystrixAdapter;
import se.avanzabank.asterix.ft.metrics.HystrixEventPublisher;


@MetaInfServices(value = AsterixFaultTolerancePlugin.class)
public class HystrixFaultTolerancePlugin implements AsterixFaultTolerancePlugin, AsterixPluginsAware {
	private AsterixPlugins plugins;
	private boolean eventPublisherRegistered = false;

	@Override
	public <T> T addFaultTolerance(Class<T> api, T provider, String group) {
		Objects.requireNonNull(api);
		Objects.requireNonNull(provider);
		Objects.requireNonNull(group);
		registerEventLogger();
		return HystrixAdapter.create(api, provider, group);
	}

	private synchronized void registerEventLogger() {
		// TODO better activation when we have activation framework :)
		if (!eventPublisherRegistered) {
			AsterixEventLoggerPlugin eventLogger = plugins.getPlugin(AsterixEventLoggerPlugin.class);
			HystrixEventPublisher publisher = new HystrixEventPublisher(eventLogger);
			HystrixAdapter.getEventNotifier().addEventNotifier(publisher);
			eventPublisherRegistered = true;
		}
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

}
