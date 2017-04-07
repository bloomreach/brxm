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
package org.onehippo.cms7.services.processor.richtext.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class JcrNodeFactoryTest {

    private MockNode root;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
    }

    @Test
    public void testRetrieveNodeByIdentifier() throws Exception {
        final NodeFactory nodeFactory = new TestNodeFactory();
        final Node node = nodeFactory.getNodeByIdentifier(root.getIdentifier());
        assertEquals(node, root);
    }

    @Test
    public void testRetrieveNodeByPath() throws Exception {
        final NodeFactory nodeFactory = new TestNodeFactory();
        final Node node = nodeFactory.getNodeByPath("/");
        assertEquals(node, root);
    }

    @Test
    public void testRetrieveNodeModelByIdentifier() throws Exception {
        final NodeFactory nodeFactory = new TestNodeFactory();
        final Model<Node> nodeModel = nodeFactory.getNodeModelByIdentifier(root.getIdentifier());
        assertEquals(nodeModel.get(), root);
    }

    @Test
    public void testRetrieveNodeModelByNode() throws Exception {
        final NodeFactory nodeFactory = new TestNodeFactory();
        assertNull(nodeFactory.getNodeModelByNode(null));

        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        assertEquals(nodeModel.get(), root);
    }

    @Test
    public void testLogMessageOnGetNodeModelByNode () throws Exception {
        final TestAppender appender = createAppender(Level.ERROR);

        final NodeFactory nodeFactory = new TestNodeFactory();
        final Node brokenNode = EasyMock.createMock(Node.class);
        expect(brokenNode.getIdentifier()).andThrow(new RepositoryException("I have no id"));
        expect(brokenNode.getPath()).andReturn("/broken-node");
        EasyMock.replay(brokenNode);

        nodeFactory.getNodeModelByNode(brokenNode);

        removeAppender(appender);
        assertLogMessage(appender, "Failed to create node model from node /broken-node", Level.ERROR);
        EasyMock.verify(brokenNode);
    }

    @Test
    public void testLogMessageOnGet() throws Exception {
        final TestAppender appender = createAppender(Level.ERROR);

        final NodeFactory nodeFactory = new TestNodeFactory() {
            @Override
            public Node getNodeByIdentifier(final String uuid) throws RepositoryException {
                throw new RepositoryException("Failed to load node");
            }
        };

        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        nodeModel.release();
        nodeModel.get();

        removeAppender(appender);
        assertLogMessage(appender, "Failed to load node with uuid cafebabe-cafe-babe-cafe-babecafebabe", Level.ERROR);
    }

    @Test
    public void testLogMessageOnSet() throws Exception {
        final TestAppender appender = createAppender(Level.ERROR);
        final NodeFactory nodeFactory = new TestNodeFactory();
        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        final Node brokenNode = EasyMock.createMock(Node.class);
        expect(brokenNode.getIdentifier()).andThrow(new RepositoryException("I have no id"));
        expect(brokenNode.getPath()).andReturn("/broken-node");
        EasyMock.replay(brokenNode);
        nodeModel.set(brokenNode);

        removeAppender(appender);
        assertLogMessage(appender, "Failed to retrieve uuid from node /broken-node", Level.ERROR);
        EasyMock.verify(brokenNode);
    }

    private void assertLogMessage(final TestAppender appender, final String message, final Level level) {
        final List<LoggingEvent> log = appender.getLog();
        final LoggingEvent logEntry = log.get(0);
        assertThat(logEntry.getLevel(), is(level));
        assertThat(logEntry.getMessage(), is(message));
    }


    private TestAppender createAppender(final Level level) {
        final TestAppender appender = new TestAppender();
        final Logger logger = Logger.getLogger(JcrNodeFactory.class);
        logger.addAppender(appender);
        logger.setLevel(level);
        return appender;
    }

    private void removeAppender(final TestAppender appender) {
        final Logger logger = Logger.getLogger(JcrNodeFactory.class);
        logger.removeAppender(appender);
    }

    class TestAppender extends AppenderSkeleton {
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

    class TestNodeFactory extends JcrNodeFactory {
        @Override
        protected Session getSession() throws RepositoryException {
            return root.getSession();
        }
    }

}
