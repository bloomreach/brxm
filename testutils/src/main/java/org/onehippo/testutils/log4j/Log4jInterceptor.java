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
package org.onehippo.testutils.log4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * Log4jInterceptor is a dynamic and {@link AutoCloseable} Log4j 2.x {@link Filter} to deny logging, and optionally
 * capturing, specific {@link LogEvent}s within a try-with-resources execution.
 * <p>
 * This can be very useful in case of unit testing some behaviour with expected exceptions, for example when unit testing
 * wrongly configured bootstrap configuration. To avoid these expected exceptions being logged a Log4jInterceptor
 * can be used to temporarily intercept and deny this from happening.
 * </p>
 * <p>
 * In addition, the Log4jInterceptor also can be used to validate specific log messages to be raised, by capturing
 * (trapping) these {@link LogEvent}s within the Log4jInterceptor, to be accessed and inspected afterwards.
 * </p>
 * <p>
 * For example assume we need to unit test a wrong Spring bean and expect a warning to be logged when starting the
 * component manager. Assume you know the warning is logged by {@code componentManager.start();} you can suppress this
 * warning as follows:
 * </p>
 * <pre><code>
 *     final SpringComponentManager componentManager = ...
 *     try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(SpringComponentManager.class).build()) {
 *           componentManager.start();
 *     }
 * </code></pre>
 * <p>
 * Or more conveniently, you can use the {@link Builder#run(Runnable)} method which takes care of the try-with-resources
 * block for you (only really useful when <em>not</em> using {@link Builder#trap()}):
 * </p>
 * <pre><code>
 *     Log4jInterceptor.onWarn().deny(SpringComponentManager.class).run( () -> {
 *           componentManager.start();
 *     });
 * </code></pre>
 * <p>
 * If you also need to check the exact message logged during this execution, you can trap and inspect them as follows:
 * </p>
 * <pre><code>
 *     try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(SpringComponentManager.class).build()) {
 *           componentManager.start();
 *           Assert.assertTrue(interceptor.messages().anyMatch(m->m.equals("some message")));
 *     }
 * </code></pre>
 * <p>
 * <em>Note: the Log4jInterceptor can and will trap/deny events regardless of the configured logger level!</em>
 * </p>
 * <p>
 * So even if a logger configuration is on level INFO, a DEBUG message will still be trapped when using {@link #onDebug},
 * which is very convenient as you are only dependent on the <em>code</em>, not the current log4j2.xml configuration to
 * trap/deny specific messages.
 * </p>
 * <p>
 * Static {@link Builder} factory methods are available for all Log4J levels: {@link #onFatal()}, {@link #onError()},
 * {@link #onWarn()}, {@link #onInfo()}, {@link #onDebug()}, {@link #onTrace()}, and {@link #onAll()}.
 * </p>
 * <p>
 * For both denying or (and) trapping logging events, {@link Builder#deny()} and {@link Builder#trap()} builder
 * methods can be used, repeatedly if so desired, to configure which (Log4J) loggers need to be intercepted.
 * </p>
 * <p>
 * When <em>only</em> the default (parameter less) {@link Builder#deny()} and/or {@link Builder#trap()} methods are
 * used logging events for all loggers will be intercepted.
 * </p>
 * <p>
 * In addition one or more logger names, or classes, can be provided as parameters to these methods.
 * </p>
 * <p>
 * When either/or one or more logges are configured, then only those loggers, <em>and</em> their <em>children</em> will
 * be denied and/or trapped.
 * </p>
 * <p>
 * For example the following Log4jInterceptor can be used to deny all logging for everything within the {@code org.onehippo}
 * and {@code com.onehippo} hierarchy at the same time:
 * </p>
 * <pre><code>
 *     Log4jInterceptor ignored = Log4jInterceptor.onAll().deny("org.onehippo","com.onehippo").build();
 * </code></pre>
 * <p>
 * When the {@link Builder#build()} method is called to create the Log4jInterceptor instance, without configuring any
 * deny or trap loggers, all logging will be denied until the Log4jInterceptor instance is (auto) closed again.
 * </p>
 * <p>
 * Note also that <em>trapping</em> a specific logger (or all) takes precedence over <em>denying</em>, so even if a
 * logger is also denied logging, the logging event(s) will still be captured (trapped).
 * </p>
 * <p>
 * In addition to accessing the trapped logging events through {@link #getEvents() List&lt;LogEvent&gt; getEvents()},
 * the convenient {@link #messages()} can be used to retrieve a stream of each event its {@link Message#getFormattedMessage()}.
 * </p>
 * <p>
 * Note: Log4jInterceptor replaces the now dropped ExecuteOnLogLevel as provided before with the hippo-repository-testutils
 * module.
 * </p>
 */
public class Log4jInterceptor implements AutoCloseable {

    private static final class InterceptorFilter extends AbstractFilter {

        private final String uuid = UUID.randomUUID().toString();
        private final Level level;
        private final Set<String> trapLoggers;
        private final Set<String> denyloggers;
        protected final List<LogEvent> events = new ArrayList<>();

        public InterceptorFilter(final Level level, final Set<String> trapLoggers, final Set<String> denyloggers) {
            super(Result.DENY, Result.NEUTRAL);
            this.level = level;
            this.trapLoggers = trapLoggers;
            this.denyloggers = (trapLoggers == null && denyloggers == null) ? Collections.emptySet() : denyloggers;
        }

        @Override
        public Result filter(final LogEvent event) {
            switch (filter(event.getLoggerName(), event.getLevel())) {
                case NEUTRAL: return Result.NEUTRAL;
                case ACCEPT: events.add(event);
            }
            return Result.DENY;
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                             final Throwable t) {
            return filter(logger.getName(), level);
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                             final Throwable t) {
            return filter(logger.getName(), level);
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                             final Object... params) {
            return filter(logger.getName(), level);
        }

        protected Result filter(final String loggerName, final Level level) {
            if (this.level.isLessSpecificThan(level)) {
                if (matchLogger(loggerName, trapLoggers)) {
                    return Result.ACCEPT;
                } else if (matchLogger(loggerName, denyloggers)) {
                    return Result.DENY;
                }
            }
            return Result.NEUTRAL;
        }

        protected boolean matchLogger(final String name, final Set<String> loggers) {
            if (loggers != null) {
                if (loggers.isEmpty() || loggers.contains(name)) {
                    return true;
                }
                for (final String parent : loggers) {
                    if (name.startsWith(parent+".")) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equalsImpl(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return uuid.equals(((InterceptorFilter) obj).uuid);
        }
    }

    public static final class Builder {
        private final Level level;
        private Set<String> trapLoggers;
        private Set<String> denyLoggers;

        protected Builder(final Level level) {
            this.level = level != null ? level : Level.ALL;
        }

        protected Set<String> getTrapLoggers() {
            if (trapLoggers == null) {
                trapLoggers = new HashSet<>();
            }
            return trapLoggers;
        }

        protected Set<String> getDenyLoggers() {
            if (denyLoggers == null) {
                denyLoggers = new HashSet<>();
            }
            return denyLoggers;
        }

        protected void addLoggers(final Class clazz, final Class[] classes, final Set<String> loggers) {
            loggers.add(clazz.getName());
            if (classes != null) {
                for (Class c : classes) {
                    loggers.add(c.getName());
                }
            }
        }
        public Builder trap() {
            getTrapLoggers();
            return this;
        }

        public Builder trap(String logger, String ... loggers) {
            getTrapLoggers().add(logger);
            getTrapLoggers().addAll(Arrays.asList(loggers));
            return this;
        }

        public Builder trap(Class clazz, Class ... classes) {
            addLoggers(clazz, classes, getTrapLoggers());
            return this;
        }

        public Builder deny() {
            getDenyLoggers();
            return this;
        }

        public Builder deny(String logger, String ... loggers) {
            getDenyLoggers().add(logger);
            getDenyLoggers().addAll(Arrays.asList(loggers));
            return this;
        }

        public Builder deny(Class clazz, Class ... classes) {
            addLoggers(clazz, classes, getDenyLoggers());
            return this;
        }

        public Log4jInterceptor build() {
            return new Log4jInterceptor(level, trapLoggers, denyLoggers);
        }

        public void run(Runnable runnable) {
            try (Log4jInterceptor ignored = build()) {
                runnable.run();
            }
        }
    }

    public static Builder onFatal() {
        return new Builder(Level.FATAL);
    }

    public static Builder onError() {
        return new Builder(Level.ERROR);
    }

    public static Builder onWarn() {
        return new Builder(Level.WARN);
    }

    public static Builder onInfo() {
        return new Builder(Level.INFO);
    }

    public static Builder onDebug() {
        return new Builder(Level.DEBUG);
    }

    public static Builder onTrace() {
        return new Builder(Level.TRACE);
    }

    public static Builder onAll() {
        return new Builder(Level.ALL);
    }

    private final InterceptorFilter filter;

    public Log4jInterceptor(final Level level, final Set<String> trapLoggers, final Set<String> denyLoggers) {
        filter = new InterceptorFilter(level, trapLoggers, denyLoggers);
        final LoggerContext context = LoggerContext.getContext(false);
        context.getLoggers();
        final Configuration config = context.getConfiguration();

        for (LoggerConfig logger : config.getLoggers().values()) {
            logger.addFilter(filter);
        }
        config.addFilter(filter);
    }

    public List<LogEvent> getEvents() {
        return filter.events;
    }

    public Stream<String> messages() {
        return getEvents().stream().map(e -> e.getMessage().getFormattedMessage());
    }

    @Override
    public void close() {
        final Configuration config = LoggerContext.getContext(false).getConfiguration();
        for (LoggerConfig logger : config.getLoggers().values()) {
            logger.removeFilter(filter);
        }
        config.removeFilter(filter);
    }
}