/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.onehippo.testutils.log4j.Log4jInterceptor;

/**
 * Utility class to wait for specific log lines to appear in a logger. Internally, the class uses
 * {@link Log4jInterceptor} which implements a {@link org.apache.logging.log4j.core.Filter} that denies captured
 * messages to be processed further (e.g. be printed to the terminal)
 * See {@link LogLineWaiter#LogLineWaiter(String)} and {@link LogLineWaiter#LogLineWaiter(String,Level)} for details.
 */
public class LogLineWaiter implements AutoCloseable {

    public static final int DEFAULT_TIMEOUT = 5000;

    private final Log4jInterceptor interceptor;

    /**
     * Constructs a waiter that waits for messages on all log levels for the given logger. This has the side effect that
     * all messages produced by the given logger are denied to be processed further (e.g. be printed to the terminal).
     */
    public LogLineWaiter(final String loggerName) throws Exception {
        interceptor = Log4jInterceptor.onAll().trap(loggerName).build();
    }

    /**
     * Constructs a waiter that waits for messages on the given log level for the given logger. This has the side
     * effect that all messages produced by the given logger are denied to be processed further (e.g. be printed to the
     * terminal).
     */
    public LogLineWaiter(final String loggerName, final Level level) throws Exception {
        if (level.equals(Level.FATAL)) {
            interceptor = Log4jInterceptor.onFatal().trap(loggerName).build();
        } else if (level.equals(Level.ERROR)) {
            interceptor = Log4jInterceptor.onError().trap(loggerName).build();
        } else if (level.equals(Level.WARN)) {
            interceptor = Log4jInterceptor.onWarn().trap(loggerName).build();
        } else if (level.equals(Level.INFO)) {
            interceptor = Log4jInterceptor.onInfo().trap(loggerName).build();
        } else if (level.equals(Level.DEBUG)) {
            interceptor = Log4jInterceptor.onDebug().trap(loggerName).build();
        } else if (level.equals(Level.TRACE)) {
            interceptor = Log4jInterceptor.onTrace().trap(loggerName).build();
        } else {
            throw new IllegalArgumentException("Unknown log level: " + level.toString());
        }
    }

    /**
     * Blocks for a maximum {@link LogLineWaiter#DEFAULT_TIMEOUT} milliseconds for the given message to be logged.
     */
    public void waitFor(final String message) throws InterruptedException, TimeoutException {
        waitFor(new String[]{message}, DEFAULT_TIMEOUT);
    }

    /**
     * Blocks for a maximum {@link LogLineWaiter#DEFAULT_TIMEOUT} milliseconds for the given messages to be logged in
     * the given order.
     */
    public void waitFor(final String[] messages) throws InterruptedException, TimeoutException {
        waitFor(messages, DEFAULT_TIMEOUT);
    }

    /**
     * Blocks for the given maximum timeout in milliseconds for the given messages to be logged in
     * the given order.
     */
    public void waitFor(final String[] messages, final int timeout) throws InterruptedException, TimeoutException {
        // events are added asynchronously to the list by the interceptor
        final List<LogEvent> events = interceptor.getEvents();

        long start = System.currentTimeMillis();
        int waitMs = 1;
        int messageIndex = 0;
        int eventIndex = 0;
        do {
            while (eventIndex < events.size()) {
                if (events.get(eventIndex).getMessage().getFormattedMessage().startsWith(messages[messageIndex])) {
                    messageIndex++;
                    if (messageIndex == messages.length) {
                        return;
                    }
                }
                eventIndex++;
            }
            Thread.sleep(waitMs);
            waitMs *= 2;
        } while (start + timeout > System.currentTimeMillis());

        final StringBuilder builder = new StringBuilder();
        builder.append("Timeout of ").append(timeout).append(" ms exceeded; did not find matches for all messages in ")
                .append(Arrays.asList(messages).toString()).append("; captured ").append(events.size())
                .append(" event messages:\n");
        serializeAllEvents(builder);

        throw new TimeoutException(builder.toString());
    }

    public String serializeAllEvents(final StringBuilder builder) {
        for (final LogEvent event : interceptor.getEvents()) {
            builder.append(Instant.ofEpochMilli(event.getTimeMillis()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT))
                    .append(' ').append(event.getMessage().getFormattedMessage()).append('\n');
        }
        return builder.toString();
    }

    @Override
    public void close() throws Exception {
        interceptor.close();
    }

}
