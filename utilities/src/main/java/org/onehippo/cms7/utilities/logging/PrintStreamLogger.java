/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.logging;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Simple slf4j {@link Logger} for logging to {@link PrintStream}s
 */
public class PrintStreamLogger extends MarkerIgnoringBase implements Logger {

    private static final long serialVersionUID = -1l;

    public static final int TRACE_LEVEL = 0;
    public static final int DEBUG_LEVEL = 1;
    public static final int INFO_LEVEL = 2;
    public static final int WARN_LEVEL = 3;
    public static final int ERROR_LEVEL = 4;

    private final PrintStream[] out;
    private final int level;

    /**
     * Maps a slf4j {@link Level#toString()} value (case-insensitive) to a matching PrintStreamLogger int level.
     * Defaults to "DEBUG" when passing in a null string.
     * @param level slf4j logging level string value
     * @return matching int value
     */
    private static int getLogLevel(final String level) {
        final String logLevel = level != null ? level.toUpperCase() : Level.DEBUG.toString();
        if (Level.DEBUG.toString().equals(logLevel)) {
            return DEBUG_LEVEL;
        } else if (Level.INFO.toString().equals(logLevel)) {
            return INFO_LEVEL;
        } else if (Level.TRACE.toString().equals(logLevel)) {
            return TRACE_LEVEL;
        } else if (Level.WARN.toString().equals(logLevel)) {
            return WARN_LEVEL;
        } else {
            return ERROR_LEVEL;
        }
    }

    public PrintStreamLogger(final String name, final String level, final PrintStream... out) throws IllegalArgumentException {
        this(name, getLogLevel(level), out);
    }

    public PrintStreamLogger(final String name, final int level, final PrintStream... out) throws IllegalArgumentException {
        this.name = name;
        if (out == null || out.length == 0) {
            throw new IllegalArgumentException("No print streams provided");
        }
        this.out = out;
        this.level = level;
    }

    protected void log(String level, String message, Throwable t) {
        print(getMessageString(level, message), t);
    }

    protected String getMessageString(String level, String message) {
        return level + " " + message;
    }

    protected void print(String s, Throwable t) {
        for (PrintStream ps : out) {
            ps.println(s);
            if (t != null) {
                t.printStackTrace(ps);
            }
            ps.flush();
        }
    }

    private void formatAndLog(String level, String format, Object arg1,
                              Object arg2) {
        FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
        log(level, tp.getMessage(), tp.getThrowable());
    }

    private void formatAndLog(String level, String format, Object[] argArray) {
        FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
        log(level, tp.getMessage(), tp.getThrowable());
    }

    public boolean isTraceEnabled() {
        return level <= TRACE_LEVEL;
    }

    public void trace(String msg) {
        if (!isTraceEnabled()) {
            return;
        }
        log(Level.TRACE.toString(), msg, null);
    }

    public void trace(String format, Object arg) {
        if (!isTraceEnabled()) {
            return;
        }
        formatAndLog(Level.TRACE.toString(), format, arg, null);
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (!isTraceEnabled()) {
            return;
        }
        formatAndLog(Level.TRACE.toString(), format, arg1, arg2);
    }

    public void trace(String format, Object[] argArray) {
        if (!isTraceEnabled()) {
            return;
        }
        formatAndLog(Level.TRACE.toString(), format, argArray);
    }

    public void trace(String msg, Throwable t) {
        if (!isTraceEnabled()) {
            return;
        }
        log(Level.TRACE.toString(), msg, t);
    }

    public boolean isDebugEnabled() {
        return level <= DEBUG_LEVEL;
    }

    public void debug(String msg) {
        if (!isDebugEnabled()) {
            return;
        }
        log(Level.DEBUG.toString(), msg, null);
    }

    public void debug(String format, Object arg) {
        if (!isDebugEnabled()) {
            return;
        }
        formatAndLog(Level.DEBUG.toString(), format, arg, null);
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (!isDebugEnabled()) {
            return;
        }
        formatAndLog(Level.DEBUG.toString(), format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
        if (!isDebugEnabled()) {
            return;
        }
        formatAndLog(Level.DEBUG.toString(), format, argArray);
    }

    public void debug(String msg, Throwable t) {
        if (!isDebugEnabled()) {
            return;
        }
        log(Level.DEBUG.toString(), msg, t);
    }

    public boolean isInfoEnabled() {
        return level <= INFO_LEVEL;
    }

    public void info(String msg) {
        if (!isInfoEnabled()) {
            return;
        }
        log(Level.INFO.toString(), msg, null);
    }

    public void info(String format, Object arg) {
        if (!isInfoEnabled()) {
            return;
        }
        formatAndLog(Level.INFO.toString(), format, arg, null);
    }

    public void info(String format, Object arg1, Object arg2) {
        if (!isInfoEnabled()) {
            return;
        }
        formatAndLog(Level.INFO.toString(), format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
        if (!isInfoEnabled()) {
            return;
        }
        formatAndLog(Level.INFO.toString(), format, argArray);
    }

    public void info(String msg, Throwable t) {
        if (!isInfoEnabled()) {
            return;
        }
        log(Level.INFO.toString(), msg, t);
    }

    public boolean isWarnEnabled() {
        return level <= WARN_LEVEL;
    }

    public void warn(String msg) {
        if (!isWarnEnabled()) {
            return;
        }
        log(Level.WARN.toString(), msg, null);
    }

    public void warn(String format, Object arg) {
        if (!isWarnEnabled()) {
            return;
        }
        formatAndLog(Level.WARN.toString(), format, arg, null);
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (!isWarnEnabled()) {
            return;
        }
        formatAndLog(Level.WARN.toString(), format, arg1, arg2);
    }

    public void warn(String format, Object[] argArray) {
        if (!isWarnEnabled()) {
            return;
        }
        formatAndLog(Level.WARN.toString(), format, argArray);
    }

    public void warn(String msg, Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        log(Level.WARN.toString(), msg, t);
    }

    public boolean isErrorEnabled() {
        return level <= ERROR_LEVEL;
    }

    public void error(String msg) {
        if (!isErrorEnabled()) {
            return;
        }
        log(Level.ERROR.toString(), msg, null);
    }

    public void error(String format, Object arg) {
        if (!isErrorEnabled()) {
            return;
        }
        formatAndLog(Level.ERROR.toString(), format, arg, null);
    }

    public void error(String format, Object arg1, Object arg2) {
        if (!isErrorEnabled()) {
            return;
        }
        formatAndLog(Level.ERROR.toString(), format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
        if (!isErrorEnabled()) {
            return;
        }
        formatAndLog(Level.ERROR.toString(), format, argArray);
    }

    public void error(String msg, Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        log(Level.ERROR.toString(), msg, t);
    }

}
