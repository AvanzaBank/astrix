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
package com.avanza.astrix.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.avanza.astrix.context.metrics.MetricsSpi;
import com.avanza.astrix.context.metrics.TimerSnaphot;
import com.avanza.astrix.context.metrics.TimerSpi;
import com.avanza.astrix.core.function.CheckedCommand;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import rx.Observable;

final class DropwizardMetrics implements MetricsSpi {

	private final AtomicInteger nextTimerId = new AtomicInteger(0);
	private final MetricRegistry metrics = new MetricRegistry();
	
	@Override
	public TimerSpi createTimer() {
		return new TimerAdapter(metrics.timer("timer-" + nextTimerId.incrementAndGet()));
	}
	
	static class TimerAdapter implements TimerSpi {
		private Timer timer;
		public TimerAdapter(Timer timer) {
			this.timer = timer;
		}

		@Override
		public <T> CheckedCommand<T> timeExecution(final CheckedCommand<T> execution) {
			return () -> {
				Context context = timer.time();
				try {
					return execution.call();
				} finally {
					context.stop();
				}
			};
		}

		@Override
		public <T> Supplier<Observable<T>> timeObservable(final Supplier<Observable<T>> observableFactory) {
			return () -> {
				final Context context = timer.time();
				return observableFactory.get().doOnTerminate(() -> context.stop());
			};
		}

		@Override
		public TimerSnaphot getSnapshot() {
			// Rates are are in seconds by default
			// Duration are in NANO_SECONDS
			TimeUnit rateUnit = TimeUnit.SECONDS;
			TimeUnit durationUnit = TimeUnit.MILLISECONDS;
			double durationFactor = 1.0 / durationUnit.toNanos(1);
//			double rateFactor = 1.0 / rateUnit.toSeconds(1);
			Snapshot snapshot = timer.getSnapshot();
			return TimerSnaphot.builder()
							   .count(timer.getCount())
							   .meanRate(timer.getMeanRate()) // No need to convert rate since its already in SECONDS 
							   .oneMinuteRate(timer.getOneMinuteRate()) // No need to convert rate since its already in SECONDS
							   .maxLatency(snapshot.getMax() * durationFactor)
							   .minLatency(snapshot.getMin() * durationFactor)
							   .set50thPercentileLatency(snapshot.getMedian() * durationFactor)
							   .set90thPercentileLatency(snapshot.getValue(0.9) * durationFactor)
							   .set99thPercentileLatency(snapshot.get99thPercentile() * durationFactor)
							   .rateUnit(rateUnit)
							   .durationUnit(durationUnit)
							   .build();
		}
		
	}
	
	// For testing
	MetricRegistry getMetrics() {
		return metrics;
	}
	
}
