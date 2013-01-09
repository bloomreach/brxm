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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.logging.LogEvent;
import org.hippoecm.hst.logging.LogEventBuffer;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LogEvent.Level;
import org.slf4j.helpers.MessageFormatter;

public class TraceToolSlf4jLogger implements Logger {
    
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private LogEventBuffer traceToolLogEventBuffer;
    private String loggerName;
    private Logger teeLogger;

    public TraceToolSlf4jLogger(final LogEventBuffer traceToolLogEventBuffer, final org.slf4j.Logger logger) {
        this.traceToolLogEventBuffer = traceToolLogEventBuffer;
        this.loggerName = logger.getName();
        this.teeLogger = new Slf4jLogger(logger);
    }
    
    public void debug(String msg) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, msg);
        teeLogger.debug(msg);
    }

    public void debug(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, arg);
        teeLogger.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, arg1, arg2);
        teeLogger.debug(format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, argArray);
        teeLogger.debug(format, argArray);
    }

    public void debug(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, msg, t); 
        teeLogger.debug(msg, t);
    }

    public void error(String msg) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, msg);
        teeLogger.error(msg);
    }

    public void error(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, arg);
        teeLogger.error(format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, arg1, arg2);
        teeLogger.error(format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, argArray);
        teeLogger.error(format, argArray);
    }

    public void error(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, msg, t);
        teeLogger.error(msg, t);
    }

    public void info(String msg) {
        addTraceToolLogEvent(LogEvent.Level.INFO, msg);
        teeLogger.info(msg);
    }

    public void info(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, arg);
        teeLogger.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, arg1, arg2);
        teeLogger.info(format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, argArray);
        teeLogger.info(format, argArray);
    }

    public void info(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.INFO, msg, t);
        teeLogger.info(msg, t);
    }

    public boolean isDebugEnabled() {
        return teeLogger.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return teeLogger.isErrorEnabled();
    }

    public boolean isInfoEnabled() {
        return teeLogger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return teeLogger.isWarnEnabled();
    }

    public void warn(String msg) {
        addTraceToolLogEvent(LogEvent.Level.WARN, msg);
        teeLogger.warn(msg);
    }

    public void warn(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, arg);
        teeLogger.warn(format, arg);
    }

    public void warn(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, argArray);
        teeLogger.warn(format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, arg1, arg2);
        teeLogger.warn(format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.WARN, msg, t);
        teeLogger.warn(msg, t);
    }

    private void addTraceToolLogEvent(Level level, String message) {
        LogEvent event = new LogEventImpl(loggerName, level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object arg) {
        String message = MessageFormatter.format(format, arg).getMessage();
        LogEvent event = new LogEventImpl(loggerName, level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object arg1, Object arg2) {
        String message = MessageFormatter.format(format, arg1, arg2).getMessage();
        LogEvent event = new LogEventImpl(loggerName, level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object [] argArray) {
        String message = MessageFormatter.arrayFormat(format, argArray).getMessage();
        LogEvent event = new LogEventImpl(loggerName, level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String message, Throwable t) {
        StringBuilder sbMessage = new StringBuilder(256).append(message).append(LINE_SEPARATOR);
        PrintWriter pw = null;
        StringWriter sw = null;
        
        try {
            sw = new StringWriter(256);
            pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            sbMessage.append(sw.toString()).append(LINE_SEPARATOR);
        } catch (Exception e) {
            sbMessage.append("Failed to print stack trace.");
        } finally {
            IOUtils.closeQuietly(pw);
            IOUtils.closeQuietly(sw);
        }
        
        LogEvent event = new LogEventImpl(loggerName, level, sbMessage.toString());
        traceToolLogEventBuffer.add(event);
    }

}
