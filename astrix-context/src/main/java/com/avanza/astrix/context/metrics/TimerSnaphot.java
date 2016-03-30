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

public class TimerSnaphot {
	
	private final long count;
	private final double _90thPercentileLatency;
	private final double _99thPercentileLatency;
	private final double _50thPercentileLatency;
	private final double maxLatency;
	private final double meanRate;
	private final double minLatency;
	private final double oneMinuteRate;
	private final TimeUnit rateUnit;
	private final TimeUnit durationUnit;
	
	private TimerSnaphot(Builder builder) {
		this.count = builder.count;
		this._50thPercentileLatency = builder._50thPercentileLatency;
		this._99thPercentileLatency = builder._99thPercentileLatency;
		this._90thPercentileLatency = builder._90thPercentileLatency;
		this.maxLatency = builder.maxLatency;
		this.minLatency = builder.minLatency;
		this.meanRate = builder.meanRate;
		this.oneMinuteRate = builder.oneMinuteRate;
		this.rateUnit = builder.rateUnit;
		this.durationUnit = builder.durationUnit;
	}

	public long getCount() {
		return this.count;
	}
	
	/**
	 * 50th percentile execution time in {@link #getDurationUnit()} for (roughly)
	 * the last five minutes. 
	 * 
	 * @return
	 */
	public double get50thPercentile() {
		return this._50thPercentileLatency;
	}

	/**
	 * The 90th percentile execution time response times in {@link #getDurationUnit()} for (roughly)
	 * the last five minutes
	 * 
	 * @return
	 */
	public double get90thPercentile() {
		return _90thPercentileLatency;
	}
	
	/**
	 * The 99th percentile execution time response times in {@link #getDurationUnit()} for (roughly)
	 * the last five minutes
	 * 
	 * @return
	 */
	public double get99thPercentileLatency() {
		return this._99thPercentileLatency;
	}
	

	/**
	 * The maximum execution time 
	 * @return
	 */
	public double getMax() {
		return this.maxLatency;
	}

	public double getMeanRate() {
		return this.meanRate;
	}

	public double getMin() {
		return this.minLatency;
	}
	

	/**
	 * Executions/second for the last minute 
	 */
	public double getOneMinuteRate() {
		return this.oneMinuteRate;
	}
	
	/**
	 * The TimeUnit for all "durations" (hardcoded to 'MILLISECONDS' in current implementation)
	 */
	public TimeUnit getDurationUnit() {
		return durationUnit;
	}
	
	
	/**
	 * The TimeUnit for all "rates" (hardcoded to 'SECONDS' in current implementation)
	 */
	public TimeUnit getRateUnit() {
		return rateUnit;
	}

	public static TimerSnaphot empty() {
		return new TimerSnaphot.Builder().build();
	}
	
	public static TimerSnaphot.Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private TimeUnit rateUnit = TimeUnit.MILLISECONDS;
		private long count;
		private double _99thPercentileLatency;
		private double _90thPercentileLatency;
		private double _50thPercentileLatency;
		private double maxLatency;
		private double minLatency;
		private double meanRate;
		private double oneMinuteRate;
		private TimeUnit durationUnit;
		
		public Builder count(long count) {
			this.count = count;
			return this;
		}
		
		public Builder rateUnit(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return this;
		}

		public Builder set99thPercentileLatency(double _99thPercentile) {
			this._99thPercentileLatency = _99thPercentile;
			return this;
		}
		
		public Builder set90thPercentileLatency(double _90thPercentile) {
			this._90thPercentileLatency = _90thPercentile;
			return this;
		}

		public Builder set50thPercentileLatency(double _50thPercentile) {
			this._50thPercentileLatency = _50thPercentile;
			return this;
		}

		public Builder maxLatency(double max) {
			this.maxLatency = max;
			return this;
		}

		public Builder meanRate(double meanRate) {
			this.meanRate = meanRate;
			return this;
		}

		public Builder minLatency(double min) {
			this.minLatency = min;
			return this;
		}

		public Builder oneMinuteRate(double oneMinuteRate) {
			this.oneMinuteRate = oneMinuteRate;
			return this;
		}
		
		public TimerSnaphot build() {
			return new TimerSnaphot(this);
		}

		public Builder durationUnit(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return this;
		}
		
	}

}
