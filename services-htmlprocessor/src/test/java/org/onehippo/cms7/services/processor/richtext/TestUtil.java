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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;

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

    public static void assertLogMessage(final Log4jInterceptor interceptor, final String message, final Level level) {
        assertThat("There is a log message", interceptor.getEvents().size(), greaterThan(0));
        final LogEvent logEntry = interceptor.getEvents().get(0);
        assertThat(logEntry.getLevel(), is(level));
        assertThat(logEntry.getMessage().getFormattedMessage(), is(message));
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
