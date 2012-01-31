/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.repository.testutils;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * With this class you can instrument unit tests to expect certain log messages to be produced, while asserting against any
 * other log messages of a warning or more severe nature to be generated.  This class is a singleton and never instantiated
 * directly, but activated using a call to #expectNone() which instructs the system to allow no warnings or errors messages
 * to occur.  It is deliberately not possible to expect log messages from an explicit source or fail on informational messages,
 * since this information is not transparently moved through all possible logging frameworks and since informational messages
 * should never be considered harmful.
 *
 * This only intended to be used within unit test frameworks (typically junit) in combination with JDK logging.  In case of
 * SLF4j usage --which is recommended-- this means adding the slf4j-jdk14 as test dependency.
 */
public class AssertLogMessage extends Handler {
    private static AssertLogMessage unitTestHandler = null;
    
    private static class Expectation {
        Pattern pattern;
        int count;
        Expectation(Pattern pattern) {
            this.pattern = pattern;
            count = 0;
        }
    }
    
    private Map<String, Expectation> expectations = createEmptyExpections();

    private AssertLogMessage() {
        LogManager logMgr = LogManager.getLogManager();
        for (Enumeration<String> iter = logMgr.getLoggerNames(); iter.hasMoreElements();) {
            String logName = iter.nextElement();
            Logger logger = Logger.getLogger(logName);
            for (Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
            }
            logger.addHandler(this);
        }
    }

    /** @exclude */
    @Override
    public void publish(LogRecord record) {
        if (expectations != null) {
            boolean matchFound = false;
            for (Expectation expectation : expectations.values()) {
                if (expectation.pattern.matcher(record.getMessage()).matches()) {
                    matchFound = true;
                    ++expectation.count;
                    break;
                }
            }
            if (!matchFound && record.getLevel().intValue() >= Level.WARNING.intValue()) {
                throw new AssertionError("Unexpected " + record.getLevel().toString().toLowerCase()
                        + " message from " + record.getLoggerName() + " to log file stating: " + record.getMessage());
            }
        }
    }

    /** @exclude */
    @Override
    public void flush() {
    }

    /** @exclude */
    @Override
    public void close() throws SecurityException {
    }

    /**
     * Enabled the checking of any messages written to the log and instruct the log to fail in case log messages of
     * level warning or higher are produced, except for the messages that match the pattern in the expectMessage
     * calls.  All expected patterns remain in effect until a particular pattern is removed with expectRemove,
     * all patterns are removed with expectNone or the assertion against any patterns is turned of with expectAny.
     * @param pattern a regular expression which matches the textual string of the log message to be expected (see #String.matches())
     */
    static public void expectMessage(String pattern) {
        if (unitTestHandler.expectations == null) {
            unitTestHandler.expectations = createEmptyExpections();
        }
        unitTestHandler.expectations.put(pattern, new Expectation(Pattern.compile(pattern)));
    }

    /**
     * Asserts that a particular message was produced somewhere in the past.  This is only in effect
     * in case a expectMessage for the particular pattern is still in effect (has not been removed using
     * expectRemove nor a expectAny had been called).  After the assertMessage call the particular
     * message is reset and deemed not to have been produced.  In other words a direct call to assertMessage
     * the second time will normally fail.
     * @param pattern the earlier exact pattern used in a expectMessage
     */
    static public void assertMessage(String pattern) {
        if (unitTestHandler.expectations != null) {
            Expectation expectation = unitTestHandler.expectations.get(pattern);
            if(expectation != null) {
                if(expectation.count == 0) {
                    throw new AssertionError("Expected log message never produced: " + pattern);
                } else {
                    expectation.count = 0;
                }
            } else {
                throw new AssertionError("Expected message never expected as requirement for assertMessage: "+pattern);
            }
        }
    }

    /**
     * Removes any earlier expected log message with the exact pattern to be expected, if one was there.  After the expectRemove
     * call, any attempt to write such a message will cause an assertion to fail.
     * @param pattern The exact string earlier passed to #expectMessage
     */
    static public void expectRemove(String pattern) {
        if (unitTestHandler.expectations != null)
            unitTestHandler.expectations.remove(pattern);
    }

    /**
     * Enabled the checking of any messages written to the log and instruct the log to fail in case any log messages of
     * level warning or higher are produced.
     */
    static public void expectNone() {
        if (unitTestHandler == null) {
            unitTestHandler = new AssertLogMessage();
        } else if (unitTestHandler.expectations == null) {
            unitTestHandler.expectations = createEmptyExpections();
        } else {
            unitTestHandler.expectations.clear();
        }
    }

    /**
     * Turns off the checking of any messages written to the log.  Will also not write any entries to a log or output and also
     * any future assertions whether messages are produced will succeed.  A subsequent call to expectNone or expectMessage will
     * re-initialize the asserting against log messages without any active patterns.
     */
    static public void expectAny() {
        unitTestHandler.expectations = null;
    }

    private static Map<String, Expectation> createEmptyExpections() {
        return new TreeMap<String, Expectation>();
    }
}
