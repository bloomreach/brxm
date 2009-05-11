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

public class Slf4jLogger implements Logger {
    
    private org.slf4j.Logger logger;

    public Slf4jLogger(final org.slf4j.Logger logger) {
        this.logger = logger;
    }
    
    public void debug(String msg) {
        logger.debug(msg);
    }

    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
        logger.debug(format, argArray);
    }

    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    public void error(String msg) {
        logger.error(msg);
    }

    public void error(String msg, Object arg) {
        logger.error(msg, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
        logger.error(format, argArray);
    }

    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    public void info(String format, Object[] arg1) {
        logger.info(format, arg1);
    }

    public void info(String msg, Throwable t) {
        logger.info(msg, t);
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

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    public void warn(String format, Object[] argArray) {
        logger.warn(format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

}
