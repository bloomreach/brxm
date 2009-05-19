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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hippoecm.hst.logging.LogEvent;
import org.hippoecm.hst.logging.LogEventBuffer;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LogEvent.Level;
import org.slf4j.helpers.MessageFormatter;

public class TraceToolSlf4jLogger implements Logger {
    
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private LogEventBuffer traceToolLogEventBuffer;
    private org.slf4j.Logger logger;

    public TraceToolSlf4jLogger(final LogEventBuffer traceToolLogEventBuffer, final org.slf4j.Logger logger) {
        this.traceToolLogEventBuffer = traceToolLogEventBuffer;
        this.logger = logger;
    }
    
    public void debug(String msg) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, msg);
        logger.debug(msg);
    }

    public void debug(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, arg);
        logger.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, arg1, arg2);
        logger.debug(format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, format, argArray);
        logger.debug(format, argArray);
    }

    public void debug(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.DEBUG, msg, t); 
        logger.debug(msg, t);
    }

    public void error(String msg) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, msg);
        logger.error(msg);
    }

    public void error(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, arg);
        logger.error(format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, arg1, arg2);
        logger.error(format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, format, argArray);
        logger.error(format, argArray);
    }

    public void error(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.ERROR, msg, t);
        logger.error(msg, t);
    }

    public void info(String msg) {
        addTraceToolLogEvent(LogEvent.Level.INFO, msg);
        logger.info(msg);
    }

    public void info(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, arg);
        logger.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, arg1, arg2);
        logger.info(format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.INFO, format, argArray);
        logger.info(format, argArray);
    }

    public void info(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.INFO, msg, t);
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
        addTraceToolLogEvent(LogEvent.Level.WARN, msg);
        logger.warn(msg);
    }

    public void warn(String format, Object arg) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, arg);
        logger.warn(format, arg);
    }

    public void warn(String format, Object[] argArray) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, argArray);
        logger.warn(format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
        addTraceToolLogEvent(LogEvent.Level.WARN, format, arg1, arg2);
        logger.warn(format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        addTraceToolLogEvent(LogEvent.Level.WARN, msg, t);
        logger.warn(msg, t);
    }

    private void addTraceToolLogEvent(Level level, String message) {
        LogEvent event = new LogEventImpl(logger.getName(), level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object arg) {
        String message = MessageFormatter.format(format, arg);
        LogEvent event = new LogEventImpl(logger.getName(), level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object arg1, Object arg2) {
        String message = MessageFormatter.format(format, arg1, arg2);
        LogEvent event = new LogEventImpl(logger.getName(), level, message);
        traceToolLogEventBuffer.add(event);
    }
    
    private void addTraceToolLogEvent(Level level, String format, Object [] argArray) {
        String message = MessageFormatter.arrayFormat(format, argArray);
        LogEvent event = new LogEventImpl(logger.getName(), level, message);
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
            if (pw != null) try { pw.close(); } catch (Exception ce) { }
            if (sw != null) try { sw.close(); } catch (Exception ce) { }
        }
        
        LogEvent event = new LogEventImpl(logger.getName(), level, sbMessage.toString());
        traceToolLogEventBuffer.add(event);
    }

}
