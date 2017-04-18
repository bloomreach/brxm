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
package org.onehippo.cms7.services.processor.richtext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.repository.mock.MockNode;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class TestUtil {

    public static Tag createTag(final String name) {
        return new TestTag(name);
    }

    public static void addChildFacetNode(final MockNode node, final String name, final String uuid) throws RepositoryException {
        final Node child = node.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }

    public static TestAppender createAppender(final Level level, final Class clazz) {
        final TestAppender appender = new TestAppender();
        final Logger logger = Logger.getLogger(clazz);
        logger.addAppender(appender);
        logger.setLevel(level);
        return appender;
    }

    public static void removeAppender(final TestAppender appender, final Class clazz) {
        final Logger logger = Logger.getLogger(clazz);
        logger.removeAppender(appender);
    }

    public static void assertLogMessage(final TestAppender appender, final String message, final Level level) {
        final List<LoggingEvent> log = appender.getLog();
        assertThat("There is a log message", log.size(), greaterThan(0));
        final LoggingEvent logEntry = log.get(0);
        assertThat(logEntry.getLevel(), is(level));
        assertThat(logEntry.getMessage(), is(message));
    }


    public static class TestAppender extends AppenderSkeleton {
        private final List<LoggingEvent> log = new ArrayList<>();

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        protected void append(final LoggingEvent loggingEvent) {
            log.add(loggingEvent);
        }

        @Override
        public void close() {
        }

        public List<LoggingEvent> getLog() {
            return new ArrayList<>(log);
        }
    }


    private static class TestTag implements Tag {

        private final String name;
        private final Map<String, String> attributes;

        TestTag(final String name) {
            this.name = name;
            attributes = new HashMap<>();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAttribute(final String name) {
            return attributes.get(name);
        }

        @Override
        public void addAttribute(final String name, final String value) {
            attributes.put(name, value);
        }

        @Override
        public boolean hasAttribute(final String name) {
            return attributes.containsKey(name);
        }

        @Override
        public void removeAttribute(final String name) {
            attributes.remove(name);
        }
    }
}
