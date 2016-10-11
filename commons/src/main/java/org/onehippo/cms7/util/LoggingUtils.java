/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.util;

import java.util.Arrays;

import org.slf4j.Logger;

/**
 * Utility methods to avoid duplication of a common pattern for logging an exception.
 *
 * We currently only support the WARN logging level.
 */
public class LoggingUtils {

    /**
     * Log a warning for an unexpected exception.
     *
     * @param logger logger to use
     * @param t      the exception
     * @param msg    a plain message
     */
    public static void warnException(final Logger logger, final Throwable t, final String msg) {
        if (logger.isDebugEnabled()) {
            logger.warn(msg, t);
        } else {
            logger.warn(msg + ": {}", t.getMessage());
        }
    }

    /**
     * Log a warning for an unexpected exception.
     *
     * @param logger logger to use
     * @param t      the exception
     * @param format message format, taking a single argument
     * @param arg    argument to interpolate into formatted message
     */
    public static void warnException(final Logger logger, final Throwable t, final String format, final Object arg) {
        if (logger.isDebugEnabled()) {
            logger.warn(format, arg, t);
        } else {
            logger.warn(format + ": {}", arg, t.getMessage());
        }
    }

    /**
     * Log a warning for an unexpected exception.
     *
     * @param logger logger to use
     * @param t      the exception
     * @param format message format, taking two arguments
     * @param arg1   first argument to interpolate into formatted message
     * @param arg2   second argument to interpolate into formatted message
     */
    public static void warnException(final Logger logger, final Throwable t, final String format, final Object arg1, final Object arg2) {
        if (logger.isDebugEnabled()) {
            logger.warn(format, arg1, arg2, t);
        } else {
            logger.warn(format + ": {}", arg1, arg2, t.getMessage());
        }
    }

    /**
     * Log a warning for an unexpected exception.
     *
     * @param logger    logger to use
     * @param t         the exception
     * @param format    message format, taking three or more arguments
     * @param arguments arguments to interpolate into formatted message
     */
    public static void warnException(final Logger logger, final Throwable t, final String format, final Object... arguments) {
        final Object[] moreArguments = Arrays.copyOf(arguments, arguments.length + 1);
        if (logger.isDebugEnabled()) {
            moreArguments[arguments.length] = t;
            logger.warn(format, moreArguments);
        } else {
            moreArguments[arguments.length] = t.getMessage();
            logger.warn(format + ": {}", moreArguments);
        }
    }
}
