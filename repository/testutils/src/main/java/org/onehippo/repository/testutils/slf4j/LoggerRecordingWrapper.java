/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.testutils.slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.ext.LoggerWrapper;

/**
 * LoggerRecordingWrapper is an SLF4J Logger Wrapper, which is location-aware by extending {@link LoggerWrapper}.
 * <P>
 * This wrapper logger can substitute for the logger of the class in context in order to
 * keep the logging records for later verification in unit tests.
 * </P>
 * <P>
 * {@link #getLogRecords()} can be used to retrieve all the recorded log information,
 * and {@link #clearLogRecords()} can be used to clear all the recorded log information.
 * </P>
 */
public class LoggerRecordingWrapper extends LoggerWrapper {

    private List<LogRecord> logRecords = Collections.synchronizedList(new LinkedList<LogRecord>());

    public LoggerRecordingWrapper(Logger logger) {
        super(logger, LoggerRecordingWrapper.class.getName());
    }

    public List<LogRecord> getLogRecords() {
        return Collections.unmodifiableList(logRecords);
    }

    public void clearLogRecords() {
        logRecords.clear();
    }

    public List<String> getTraceMessages() {
        return getFormattedMessages(Level.TRACE_INT);
    }

    public List<String> getDebugMessages() {
        return getFormattedMessages(Level.DEBUG_INT);
    }

    public List<String> getInfoMessages() {
        return getFormattedMessages(Level.INFO_INT);
    }

    public List<String> getWarnMessages() {
        return getFormattedMessages(Level.WARN_INT);
    }

    public List<String> getErrorMessages() {
        return getFormattedMessages(Level.ERROR_INT);
    }

    public List<String> getFormattedMessages(final int level) {
        List<String> messages = new ArrayList();
        for (LogRecord record : logRecords) {
            if (record.getLevel() == level) {
                messages.add(record.getFormattedMessage());
            }
        }
        return Collections.unmodifiableList(messages);
    }

    @Override
    public void trace(String msg) {
        logRecords.add(new LogRecord(null, Level.TRACE_INT, msg, null, null));
        super.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logRecords.add(new LogRecord(null, Level.TRACE_INT, format, new Object [] { arg }, null));
        super.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(null, Level.TRACE_INT, format, new Object [] { arg1, arg2 }, null));
        super.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object[] argArray) {
        logRecords.add(new LogRecord(null, Level.TRACE_INT, format, argArray, null));
        super.trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logRecords.add(new LogRecord(null, Level.TRACE_INT, msg, null, t));
        super.trace(msg, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logRecords.add(new LogRecord(marker, Level.TRACE_INT, msg, null, null));
        super.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logRecords.add(new LogRecord(marker, Level.TRACE_INT, format, new Object [] { arg }, null));
        super.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(marker, Level.TRACE_INT, format, new Object [] { arg1, arg2 }, null));
        super.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        logRecords.add(new LogRecord(marker, Level.TRACE_INT, format, argArray, null));
        super.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logRecords.add(new LogRecord(marker, Level.TRACE_INT, msg, null, t));
        super.trace(marker, msg, t);
    }

    @Override
    public void debug(String msg) {
        logRecords.add(new LogRecord(null, Level.DEBUG_INT, msg, null, null));
        super.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logRecords.add(new LogRecord(null, Level.DEBUG_INT, format, new Object [] { arg }, null));
        super.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(null, Level.DEBUG_INT, format, new Object [] { arg1, arg2 }, null));
        super.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object[] argArray) {
        logRecords.add(new LogRecord(null, Level.DEBUG_INT, format, argArray, null));
        super.debug(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logRecords.add(new LogRecord(null, Level.DEBUG_INT, msg, null, t));
        super.debug(msg, t);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logRecords.add(new LogRecord(marker, Level.DEBUG_INT, msg, null, null));
        super.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logRecords.add(new LogRecord(marker, Level.DEBUG_INT, format, new Object [] { arg }, null));
        super.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(marker, Level.DEBUG_INT, format, new Object [] { arg1, arg2 }, null));
        super.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        logRecords.add(new LogRecord(marker, Level.DEBUG_INT, format, argArray, null));
        super.debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logRecords.add(new LogRecord(marker, Level.DEBUG_INT, msg, null, t));
        super.debug(marker, msg, t);
    }

    @Override
    public void info(String msg) {
        logRecords.add(new LogRecord(null, Level.INFO_INT, msg, null, null));
        super.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logRecords.add(new LogRecord(null, Level.INFO_INT, format, new Object [] { arg }, null));
        super.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(null, Level.INFO_INT, format, new Object [] { arg1, arg2 }, null));
        super.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray) {
        logRecords.add(new LogRecord(null, Level.INFO_INT, format, argArray, null));
        super.info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        logRecords.add(new LogRecord(null, Level.INFO_INT, msg, null, t));
        super.info(msg, t);
    }

    @Override
    public void info(Marker marker, String msg) {
        logRecords.add(new LogRecord(marker, Level.INFO_INT, msg, null, null));
        super.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logRecords.add(new LogRecord(marker, Level.INFO_INT, format, new Object [] { arg }, null));
        super.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(marker, Level.INFO_INT, format, new Object [] { arg1, arg2 }, null));
        super.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        logRecords.add(new LogRecord(marker, Level.INFO_INT, format, argArray, null));
        super.info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logRecords.add(new LogRecord(marker, Level.INFO_INT, msg, null, t));
        super.info(marker, msg, t);
    }

    @Override
    public void warn(String msg) {
        logRecords.add(new LogRecord(null, Level.WARN_INT, msg, null, null));
        super.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logRecords.add(new LogRecord(null, Level.WARN_INT, format, new Object [] { arg }, null));
        super.warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(null, Level.WARN_INT, format, new Object [] { arg1, arg2 }, null));
        super.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object[] argArray) {
        logRecords.add(new LogRecord(null, Level.WARN_INT, format, argArray, null));
        super.warn(format, argArray);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logRecords.add(new LogRecord(null, Level.WARN_INT, msg, null, t));
        super.warn(msg, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logRecords.add(new LogRecord(marker, Level.WARN_INT, msg, null, null));
        super.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logRecords.add(new LogRecord(marker, Level.WARN_INT, format, new Object [] { arg }, null));
        super.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(marker, Level.WARN_INT, format, new Object [] { arg1, arg2 }, null));
        super.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        logRecords.add(new LogRecord(marker, Level.WARN_INT, format, argArray, null));
        super.warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logRecords.add(new LogRecord(marker, Level.WARN_INT, msg, null, t));
        super.warn(marker, msg, t);
    }

    @Override
    public void error(String msg) {
        logRecords.add(new LogRecord(null, Level.ERROR_INT, msg, null, null));
        super.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logRecords.add(new LogRecord(null, Level.ERROR_INT, format, new Object [] { arg }, null));
        super.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(null, Level.ERROR_INT, format, new Object [] { arg1, arg2 }, null));
        super.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray) {
        logRecords.add(new LogRecord(null, Level.ERROR_INT, format, argArray, null));
        super.error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        logRecords.add(new LogRecord(null, Level.ERROR_INT, msg, null, t));
        super.error(msg, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        logRecords.add(new LogRecord(marker, Level.ERROR_INT, msg, null, null));
        super.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logRecords.add(new LogRecord(marker, Level.ERROR_INT, format, new Object [] { arg }, null));
        super.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logRecords.add(new LogRecord(marker, Level.ERROR_INT, format, new Object [] { arg1, arg2 }, null));
        super.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        logRecords.add(new LogRecord(marker, Level.ERROR_INT, format, argArray, null));
        super.error(marker, format, argArray);
    }

    public void error(Marker marker, String msg, Throwable t) {
        logRecords.add(new LogRecord(marker, Level.ERROR_INT, msg, null, t));
        super.error(marker, msg, t);
    }

}
