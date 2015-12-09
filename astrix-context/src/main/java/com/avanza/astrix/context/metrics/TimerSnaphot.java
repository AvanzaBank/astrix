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
	private final double _99thPercentile;
	private final double _50thPercentile;
	private final double max;
	private final double mean;
	private final double min;
	private final double oneMinuteRate;
	private final TimeUnit rateUnit;
	private final TimeUnit durationUnit;
	
	private TimerSnaphot(Builder builder) {
		this.count = builder.count;
		this._50thPercentile = builder._50thPercentile;
		this._99thPercentile = builder._99thPercentile;
		this.max = builder.max;
		this.mean = builder.mean;
		this.min = builder.min;
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
		return this._50thPercentile;
	}

	/**
	 * The 99th percentile execution time response times in {@link #getDurationUnit()} for (roughly)
	 * the last five minutes
	 * 
	 * @return
	 */
	public double get99thPercentile() {
		return this._99thPercentile;
	}

	/**
	 * The maximum execution time 
	 * @return
	 */
	public double getMax() {
		return this.max;
	}

	public double getMean() {
		return this.mean;
	}

	public double getMin() {
		return this.min;
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
		private double _99thPercentile;
		private double _50thPercentile;
		private double max;
		private double mean;
		private double min;
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

		public Builder set99thPercentile(double _99thPercentile) {
			this._99thPercentile = _99thPercentile;
			return this;
		}

		public Builder set50thPercentile(double _50thPercentile) {
			this._50thPercentile = _50thPercentile;
			return this;
		}

		public Builder max(double max) {
			this.max = max;
			return this;
		}

		public Builder mean(double mean) {
			this.mean = mean;
			return this;
		}

		public Builder min(double min) {
			this.min = min;
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
