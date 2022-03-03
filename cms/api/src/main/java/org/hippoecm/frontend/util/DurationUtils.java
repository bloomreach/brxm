/*
 *  Copyright 2022 Bloomreach
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.util;

import java.time.Duration;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurationUtils {

    private static final Logger log = LoggerFactory.getLogger(DurationUtils.class);

    private DurationUtils() {}

    /**
     * <p>Since Wicket 9, a Duration can no longer be constructed by the custom format specified in
     * {@link org.apache.wicket.util.time.Duration}, e.g. "8 hours" or "5 seconds". Instead, Wicket now uses the
     * JDK's {@link java.time} package and durations are parsed using the ISO-8601 duration format PnDTnHnMn.nS with
     * days considered to be exactly 24 hours.
     * </p>
     * <p>
     * For backwards compatibility, we first try the new format and fallback to the deprecated format in case of an
     * error.
     * </p>
     *
     * @param durationAsString The duration formatted as a string.
     * @return A new {@link java.time.Duration} based on the input string
     */
    public static Duration parse(final String durationAsString) {
        try {
            return StringValue.valueOf(durationAsString).toDuration();
        } catch (StringValueConversionException e) {
            log.warn("Failed to parse '{}' as a java.time.Duration.\nNote: since Wicket 9 durations are parsed by " +
                    "java.time.Duration.parse() instead of org.apache.wicket.util.time.Duration.valueOf(). " +
                    "The input formats accepted are based on the ISO-8601 duration format PnDTnHnMn.nS with days " +
                    "considered to be exactly 24 hours.\nUsers are encouraged to update their duration(s) to the new " +
                    "format. For backwards compatibility we will try to parse it using the 'deprecated' format.",
                    durationAsString);

            return org.apache.wicket.util.time.Duration.valueOf(durationAsString).toJavaDuration();
        }
    }


}
