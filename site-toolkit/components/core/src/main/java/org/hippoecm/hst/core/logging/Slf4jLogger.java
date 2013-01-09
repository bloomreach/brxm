/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
    private final static String DEFAULT_FQCN = Slf4jLogger.class.getName();
    
    private final static int DEBUG_INT = 10;
    private final static int INFO_INT = 20;
    private final static int WARN_INT = 30;
    private final static int ERROR_INT = 40;

    private final org.slf4j.Logger logger;
    private final String fqcn;
    private boolean locationAware;

    public Slf4jLogger(final org.slf4j.Logger logger) {
        this(logger, DEFAULT_FQCN);
    }
    
    public Slf4jLogger(final org.slf4j.Logger logger, final String fqcn) {
        this.logger = logger;
        this.fqcn = fqcn;
        this.locationAware = (logger instanceof LocationAwareLogger);
    }

    public void debug(String msg) {
        if (!locationAware) {
            logger.debug(msg);
        } else {
            invokeLocationAwareLoggerLogMethod(DEBUG_INT, msg, null, null);
        }
    }

    public void debug(String format, Object arg) {
        if (!locationAware) {
            logger.debug(format, arg);
        } else {
            invokeLocationAwareLoggerLogMethod(DEBUG_INT, format, new Object[] { arg }, null);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (!locationAware) {
            logger.debug(format, arg1, arg2);
        } else {
            invokeLocationAwareLoggerLogMethod(DEBUG_INT, format, new Object[] { arg1, arg2 }, null);
        }
    }

    public void debug(String format, Object[] argArray) {
        if (!locationAware) {
            logger.debug(format, argArray);
        } else {
            invokeLocationAwareLoggerLogMethod(DEBUG_INT, format, argArray, null);
        }
    }

    public void debug(String msg, Throwable t) {
        if (!locationAware) {
            logger.debug(msg, t);
        } else {
            invokeLocationAwareLoggerLogMethod(DEBUG_INT, msg, null, t);
        }
    }

    public void info(String msg) {
        if (!locationAware) {
            logger.info(msg);
        } else {
            invokeLocationAwareLoggerLogMethod(INFO_INT, msg, null, null);
        }
    }

    public void info(String format, Object arg) {
        if (!locationAware) {
            logger.info(format, arg);
        } else {
            invokeLocationAwareLoggerLogMethod(INFO_INT, format, new Object[] { arg }, null);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (!locationAware) {
            logger.info(format, arg1, arg2);
        } else {
            invokeLocationAwareLoggerLogMethod(INFO_INT, format, new Object[] { arg1, arg2 }, null);
        }
    }

    public void info(String format, Object[] argArray) {
        if (!locationAware) {
            logger.info(format, argArray);
        } else {
            invokeLocationAwareLoggerLogMethod(INFO_INT, format, argArray, null);
        }
    }

    public void info(String msg, Throwable t) {
        if (!locationAware) {
            logger.info(msg, t);
        } else {
            invokeLocationAwareLoggerLogMethod(INFO_INT, msg, null, t);
        }
    }

    public void warn(String msg) {
        if (!locationAware) {
            logger.warn(msg);
        } else {
            invokeLocationAwareLoggerLogMethod(WARN_INT, msg, null, null);
        }
    }

    public void warn(String format, Object arg) {
        if (!locationAware) {
            logger.warn(format, arg);
        } else {
            invokeLocationAwareLoggerLogMethod(WARN_INT, format, new Object[] { arg }, null);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (!locationAware) {
            logger.warn(format, arg1, arg2);
        } else {
            invokeLocationAwareLoggerLogMethod(WARN_INT, format, new Object[] { arg1, arg2 }, null);
        }
    }

    public void warn(String format, Object[] argArray) {
        if (!locationAware) {
            logger.warn(format, argArray);
        } else {
            invokeLocationAwareLoggerLogMethod(WARN_INT, format, argArray, null);
        }
    }

    public void warn(String msg, Throwable t) {
        if (!locationAware) {
            logger.warn(msg, t);
        } else {
            invokeLocationAwareLoggerLogMethod(WARN_INT, msg, null, t);
        }
    }

    public void error(String msg) {
        if (!locationAware) {
            logger.error(msg);
        } else {
            invokeLocationAwareLoggerLogMethod(ERROR_INT, msg, null, null);
        }
    }

    public void error(String format, Object arg) {
        if (!locationAware) {
            logger.error(format, arg);
        } else {
            invokeLocationAwareLoggerLogMethod(ERROR_INT, format, new Object[] { arg }, null);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (!locationAware) {
            logger.error(format, arg1, arg2);
        } else {
            invokeLocationAwareLoggerLogMethod(ERROR_INT, format, new Object[] { arg1, arg2 }, null);
        }
    }

    public void error(String format, Object[] argArray) {
        if (!locationAware) {
            logger.error(format, argArray);
        } else {
            invokeLocationAwareLoggerLogMethod(ERROR_INT, format, argArray, null);
        }
    }

    public void error(String msg, Throwable t) {
        if (!locationAware) {
            logger.error(msg, t);
        } else {
            invokeLocationAwareLoggerLogMethod(ERROR_INT, msg, null, t);
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

    private void invokeLocationAwareLoggerLogMethod(int level, String format, Object[] argArray, Throwable t) {
        try {
            String msg = null;
            
            if (argArray == null || argArray.length == 0) {
                msg = format;
            } else {
                msg = MessageFormatter.arrayFormat(format, argArray).getMessage();
            }
            
            ((LocationAwareLogger) logger).log(null, fqcn, level, msg, null, t);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to invoke location aware logger", e);
            } else {
                logger.warn("Failed to invoke location aware logger. {}", e.toString());
            }
        }
    }
}
