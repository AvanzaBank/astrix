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
package com.avanza.astrix.gs.config;

import static java.util.Objects.requireNonNull;

import org.openspaces.core.space.UrlSpaceConfigurer;

public final class SpaceConfigQueryProcessors {

    private static final String SPACE_CONFIG_QUERY_PROCESSOR_DATE_FORMAT = "space-config.QueryProcessor.date_format";
    private static final String SPACE_CONFIG_QUERY_PROCESSOR_TIME_FORMAT = "space-config.QueryProcessor.time_format";
    private static final String SPACE_CONFIG_QUERY_PROCESSOR_DATETIME_FORMAT = "space-config.QueryProcessor.datetime_format";

    private SpaceConfigQueryProcessors() {
    }

    /**
     * The default values for Java 8 dates (LocalDate, DateTime and LocalDateTime) are faulty in GigaSpaces 10.1.
     * This method helps to ensure that the default values are overridden when setting up and embedded space using
     * the given UrlSpaceConfigurer.
     *
     * @param urlSpaceConfigurer
     */

    public static void overrideDateFormatDefaults(UrlSpaceConfigurer urlSpaceConfigurer) {
        requireNonNull(urlSpaceConfigurer);

        urlSpaceConfigurer.addParameter(SPACE_CONFIG_QUERY_PROCESSOR_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss");
        urlSpaceConfigurer.addParameter(SPACE_CONFIG_QUERY_PROCESSOR_TIME_FORMAT, "HH:mm:ss");
        urlSpaceConfigurer.addParameter(SPACE_CONFIG_QUERY_PROCESSOR_DATETIME_FORMAT, "yyyy-MM-dd HH:mm:ss");
    }
}
