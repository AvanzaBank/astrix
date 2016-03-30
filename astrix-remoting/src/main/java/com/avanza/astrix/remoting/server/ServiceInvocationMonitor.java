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
package com.avanza.astrix.remoting.server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.avanza.astrix.context.metrics.Timer;
import com.avanza.astrix.core.function.Command;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;

public class ServiceInvocationMonitor implements ServiceInvocationMonitorMBean {
	
	private Timer timer;
	private AtomicLong errorCount = new AtomicLong();
	
	public ServiceInvocationMonitor(Timer timer) {
		this.timer = timer;
	}
	
	public Command<AstrixServiceInvocationResponse> monitor(Command<AstrixServiceInvocationResponse> execution) {
		return () -> {
			AstrixServiceInvocationResponse result = timer.timeExecution(execution).call();
			if (result.getThrownExceptionType() != null) {
				errorCount.incrementAndGet();
			}
			return result;
		};
	}
	
	public ServiceInvocationMonitor(Timer timer, AtomicLong errorCount) {
		this.timer = timer;
		this.errorCount = errorCount;
	}

	@Override
	public long getInvocationCount() {
		return timer.getCount();
	}

	@Override
	public long getErrorCount() {
		return errorCount.get();
	}

	@Override
	public double get50thPercentile() {
		return timer.get50thPercentileLatency();
	}
	
	@Override
	public double get90thPercentile() {
		return timer.get90thPercentileLatency();
	}

	@Override
	public double get99thPercentile() {
		return timer.get99thPercentileLatency();
	}

	@Override
	public double getMax() {
		return timer.getMax();
	}

	@Override
	public double getMeanRate() {
		return timer.getMeanRate();
	}

	@Override
	public double getMin() {
		return timer.getMin();
	}

	@Override
	public double getOneMinuteRate() {
		return timer.getOneMinuteRate();
	}

	@Override
	public TimeUnit getRateUnit() {
		return timer.getRateUnit();
	}

	@Override
	public TimeUnit getDurationUnit() {
		return timer.getDurationUnit();
	}

}
