/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.log4j;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
/**
 * A simple and {@link AutoCloseable} custom Log4j 1.x {@link Appender} capturing {@link LoggingEvent}s.
 * <p>
 * Convenient static factory methods are available for all Log4J levels: {@link #onFatal()}, {@link #onError()},
 * {@link #onWarn()}, {@link #onInfo()}, {@link #onDebug()}, {@link #onTrace()}, {@link #onAll()}
 * </p>
 * <p>
 * Besides {@link #getEvents() List&lt;LoggingEvent&gt; getEvents()}, {@link #messages()}
 * can be used to retrieve a stream of {@link LoggingEvent#getRenderedMessage()}s.
 * </p>
 * <p>
 * Typical usage is within a unit-test try-with-resources block like:
 * <pre>
 *     try (Log4jListener listener = Log4jListener.onWarn()) {
 *         // do stuff expected to log a warn level "some message" message
 *         Assert.assertTrue(listener.messages().anyMatch(m->m.equals("some message")));
 *     }
 * </pre>
 * </p>
 * TODO: Use this later from a shared test util project where this code will go to
 */
public class Log4jListener implements AutoCloseable {
    private static class Listener extends AppenderSkeleton implements Appender {
        @Override
        protected void append(LoggingEvent event) {
            events.add(event);
        }
        @Override
        public void close() { }
        @Override
        public boolean requiresLayout() { return false; }
        protected final List<LoggingEvent> events = new ArrayList<>();
    }
    public static Log4jListener onFatal() {
        return new Log4jListener(Level.FATAL);
    }
    public static Log4jListener onError() {
        return new Log4jListener(Level.ERROR);
    }
    public static Log4jListener onWarn() {
        return new Log4jListener(Level.WARN);
    }
    public static Log4jListener onInfo() {
        return new Log4jListener(Level.INFO);
    }
    public static Log4jListener onDebug() {
        return new Log4jListener(Level.DEBUG);
    }
    public static Log4jListener onTrace() {
        return new Log4jListener(Level.TRACE);
    }
    public static Log4jListener onAll() {
        return new Log4jListener(Level.ALL);
    }
    private final Listener listener;
    public Log4jListener(Level level) {
        listener = new Listener();
        listener.setThreshold(level);
        Logger.getRootLogger().addAppender(listener);
    }
    public List<LoggingEvent> getEvents() {
        return listener.events;
    }
    public Stream<String> messages() {
        return getEvents().stream().map(LoggingEvent::getRenderedMessage);
    }
    @Override
    public void close() throws Exception {
        Logger.getRootLogger().removeAppender(listener);
    }
}