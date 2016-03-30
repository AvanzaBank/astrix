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
package com.avanza.astrix.context.metrics;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.core.function.Command;

import rx.Observable;

public class Timer {
	
	private TimerSpi timerSpi;
	
	public Timer(TimerSpi timerSpi) {
		this.timerSpi = timerSpi;
	}

	public <T> Command<T> timeExecution(Command<T> execution) {
		final CheckedCommand<T> command = timerSpi.timeExecution(execution);
		return () -> {
			try {
				return command.call();
			} catch (RuntimeException e1) {
				throw e1;
			} catch (Throwable e2) {
				throw new RuntimeException(e2);
			}
		};
	}

	public <T> CheckedCommand<T> timeCheckedExecution(CheckedCommand<T> execution) {
		return timerSpi.timeExecution(execution);
	}
	
	public <T> Supplier<Observable<T>> timeObservable(Supplier<Observable<T>> command) {
		return timerSpi.timeObservable(command);
	}
	
	public double get50thPercentileLatency() {
		return timerSpi.getSnapshot().get50thPercentile();
	}
	
	public double get90thPercentileLatency() {
		return timerSpi.getSnapshot().get90thPercentile();
	}

	public double get99thPercentileLatency() {
		return timerSpi.getSnapshot().get99thPercentileLatency();
	}

	public double getMax() {
		return timerSpi.getSnapshot().getMax();
	}

	public double getMeanRate() {
		return timerSpi.getSnapshot().getMeanRate();
	}

	public double getMin() {
		return timerSpi.getSnapshot().getMin();
	}

	public double getOneMinuteRate() {
		return timerSpi.getSnapshot().getOneMinuteRate();
	}

	public long getCount() {
		return timerSpi.getSnapshot().getCount();
	}

	public TimeUnit getRateUnit() {
		return timerSpi.getSnapshot().getRateUnit();
	}

	public TimeUnit getDurationUnit() {
		return timerSpi.getSnapshot().getDurationUnit();
	}

}
