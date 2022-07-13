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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import rx.Observable;

final class AstrixMetricsImpl implements MetricsSpi {

	private final AtomicInteger nextTimerId = new AtomicInteger(0);
	private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
	
	@Override
	public TimerSpi createTimer() {
		return new TimerAdapter(
				Timer.builder("astrix.timer-" + nextTimerId.incrementAndGet())
						.publishPercentiles(0d, 0.5d, 0.9d, 0.99d)
						.register(meterRegistry)
		);
	}
	
	static class TimerAdapter implements TimerSpi {
		private final Timer timer;
		public TimerAdapter(Timer timer) {
			this.timer = timer;
		}

		@Override
		public <T> CheckedCommand<T> timeExecution(final CheckedCommand<T> execution) {
			return () -> {
				final var timerSample = Timer.start();
				try {
					return execution.call();
				} finally {
					timerSample.stop(timer);
				}
			};
		}

		@Override
		public <T> Supplier<Observable<T>> timeObservable(final Supplier<Observable<T>> observableFactory) {
			return () -> {
				final var timerSample = Timer.start();
				return observableFactory.get().doOnTerminate(() -> timerSample.stop(timer));
			};
		}

		@Override
		public TimerSnaphot getSnapshot() {
			// Rates are in seconds by default
			// Duration are in NANO_SECONDS
			TimeUnit durationUnit = TimeUnit.MILLISECONDS;
			final var snapshot = timer.takeSnapshot();
			return TimerSnaphot.builder()
							   .count(snapshot.count())
							   .maxLatency(snapshot.max(durationUnit))
							   .minLatency(getPercentile(snapshot, durationUnit, 0d))
							   .set50thPercentileLatency(getPercentile(snapshot, durationUnit, 0.5d))
							   .set90thPercentileLatency(getPercentile(snapshot, durationUnit, 0.9d))
							   .set99thPercentileLatency(getPercentile(snapshot, durationUnit, 0.99d))
							   .durationUnit(durationUnit)
							   .build();
		}

		private double getPercentile(HistogramSnapshot snapshot, TimeUnit unit, double percentile) {
			for (ValueAtPercentile v : snapshot.percentileValues()) {
				if (v.percentile() == percentile) {
					return v.value(unit);
				}
			}
			return 0d;
		}
	}
}
