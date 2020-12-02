/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage tracing/monitoring utility.
 * <p>
 * <em>
 *   Note: This is turned off by default. Set '-DTraceMonitor.enabled=true' system property to turn it on.
 * </em>
 * </p>
 */
public class TraceMonitor {

    private static final Logger log = LoggerFactory.getLogger(TraceMonitor.class);

    private static final String ENABLED_PROP = TraceMonitor.class.getSimpleName() + ".enabled";
    private static final Boolean ENABLED = BooleanUtils.toBoolean(System.getProperty(ENABLED_PROP));

    static {
        if (ENABLED) {
            log.info("TraceMonitor is enabled");
        }
    }

    protected static String getStackTrace() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }

    public static boolean isEnabled() {
        return ENABLED;
    }
}
