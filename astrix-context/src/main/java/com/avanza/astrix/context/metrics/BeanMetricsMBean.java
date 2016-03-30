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

public interface BeanMetricsMBean {

	TimeUnit getRateUnit();

	TimeUnit getDurationUnit();

	double getOneMinuteRate();

	double getMeanRate();
	
	double getMin();

	double getMax();

	double get99thPercentile();

	double get90thPercentile();

	double get50thPercentile();

	long getCount();
	
	

}
