/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.logging;

import org.hippoecm.hst.logging.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

public class Slf4jLogger implements Logger {

    /**
     * Following the pattern discussed in pages 162 through 168 of "The complete
     * log4j manual".
     */
    private final static String FQCN = Slf4jLogger.class.getName();

    private org.slf4j.Logger logger;
    private LocationAwareLogger locationAwareLogger;

    public Slf4jLogger(final org.slf4j.Logger logger) {
        this.logger = logger;

        if (logger instanceof LocationAwareLogger) {
            this.locationAwareLogger = (LocationAwareLogger) logger;
        }
    }

    public void debug(String msg) {
        if (locationAwareLogger == null) {
            logger.debug(msg);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null);
        }
    }

    public void debug(String format, Object arg) {
        if (locationAwareLogger == null) {
            logger.debug(format, arg);
        } else {
            String msg = MessageFormatter.format(format, arg);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (locationAwareLogger == null) {
            logger.debug(format, arg1, arg2);
        } else {
            String msg = MessageFormatter.format(format, arg1, arg2);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null);
        }
    }

    public void debug(String format, Object[] argArray) {
        if (locationAwareLogger == null) {
            logger.debug(format, argArray);
        } else {
            String msg = MessageFormatter.format(format, argArray);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null);
        }
    }

    public void debug(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            logger.debug(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, t);
        }
    }

    public void info(String msg) {
        if (locationAwareLogger == null) {
            logger.info(msg);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null);
        }
    }

    public void info(String format, Object arg) {
        if (locationAwareLogger == null) {
            logger.info(format, arg);
        } else {
            String msg = MessageFormatter.format(format, arg);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (locationAwareLogger == null) {
            logger.info(format, arg1, arg2);
        } else {
            String msg = MessageFormatter.format(format, arg1, arg2);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null);
        }
    }

    public void info(String format, Object[] argArray) {
        if (locationAwareLogger == null) {
            logger.info(format, argArray);
        } else {
            String msg = MessageFormatter.format(format, argArray);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null);
        }
    }

    public void info(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            logger.info(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, t);
        }
    }

    public void warn(String msg) {
        if (locationAwareLogger == null) {
            logger.warn(msg);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null);
        }
    }

    public void warn(String format, Object arg) {
        if (locationAwareLogger == null) {
            logger.warn(format, arg);
        } else {
            String msg = MessageFormatter.format(format, arg);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (locationAwareLogger == null) {
            logger.warn(format, arg1, arg2);
        } else {
            String msg = MessageFormatter.format(format, arg1, arg2);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null);
        }
    }

    public void warn(String format, Object[] argArray) {
        if (locationAwareLogger == null) {
            logger.warn(format, argArray);
        } else {
            String msg = MessageFormatter.format(format, argArray);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null);
        }
    }

    public void warn(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            logger.warn(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, t);
        }
    }

    public void error(String msg) {
        if (locationAwareLogger == null) {
            logger.error(msg);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null);
        }
    }

    public void error(String format, Object arg) {
        if (locationAwareLogger == null) {
            logger.error(format, arg);
        } else {
            String msg = MessageFormatter.format(format, arg);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (locationAwareLogger == null) {
            logger.error(format, arg1, arg2);
        } else {
            String msg = MessageFormatter.format(format, arg1, arg2);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null);
        }
    }

    public void error(String format, Object[] argArray) {
        if (locationAwareLogger == null) {
            logger.error(format, argArray);
        } else {
            String msg = MessageFormatter.format(format, argArray);
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null);
        }
    }

    public void error(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            logger.error(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, t);
        }
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

}
