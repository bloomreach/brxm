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
package org.hippoecm.hst.util;

import org.hippoecm.hst.logging.Logger;

/**
 * A {@link Logger} implementation with no operation. 
 * 
 * @version $Id$
 */
public class NOOPLogger implements Logger {

    public void debug(String msg) {
    }

    public void debug(String format, Object arg) {
    }

    public void debug(String format, Object arg1, Object arg2) {
    }

    public void debug(String format, Object[] argArray) {
    }

    public void debug(String msg, Throwable t) {
    }

    public void error(String msg) {
    }

    public void error(String format, Object arg) {
    }

    public void error(String format, Object arg1, Object arg2) {
    }

    public void error(String format, Object[] argArray) {
    }

    public void error(String msg, Throwable t) {
    }

    public void info(String msg) {
    }

    public void info(String format, Object arg) {
    }

    public void info(String format, Object arg1, Object arg2) {
    }

    public void info(String format, Object[] arg1) {
    }

    public void info(String msg, Throwable t) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public boolean isWarnEnabled() {
        return false;
    }

    public void warn(String msg) {
    }

    public void warn(String format, Object arg) {
    }

    public void warn(String format, Object[] argArray) {
    }

    public void warn(String format, Object arg1, Object arg2) {
    }

    public void warn(String msg, Throwable t) {
    }

}
