/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.logging.log4j.Level;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil.assertLogMessage;

public class JcrNodeFactoryTest {

    private MockNode root;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
    }

    @Test
    public void testRetrieveNodeByIdentifier() throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        final Node node = nodeFactory.getNodeByIdentifier(root.getIdentifier());
        assertEquals(node, root);
    }

    @Test
    public void testRetrieveNodeByPath() throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        final Node node = nodeFactory.getNodeByPath("/");
        assertEquals(node, root);
    }

    @Test
    public void testRetrieveNodeModelByIdentifier() throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        final Model<Node> nodeModel = nodeFactory.getNodeModelByIdentifier(root.getIdentifier());
        assertEquals(nodeModel.get(), root);
    }

    @Test
    public void testRetrieveNodeModelByNode() throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        assertNull(nodeFactory.getNodeModelByNode(null));

        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        assertEquals(nodeModel.get(), root);
    }

    @Test
    public void testLogMessageOnGetNodeModelByNode () throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        final Node brokenNode = EasyMock.createMock(Node.class);
        expect(brokenNode.getIdentifier()).andThrow(new RepositoryException("I have no id"));
        expect(brokenNode.getPath()).andReturn("/broken-node");
        EasyMock.replay(brokenNode);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrNodeFactory.class).build()) {
            nodeFactory.getNodeModelByNode(brokenNode);
            assertLogMessage(interceptor, "Failed to create node model from node /broken-node", Level.ERROR);
        }
        EasyMock.verify(brokenNode);
    }

    @Test
    public void testLogMessageOnGet() throws Exception {
        final NodeFactory nodeFactory = new JcrNodeFactory(() -> root.getSession()) {
            @Override
            public Node getNodeByIdentifier(final String uuid) throws RepositoryException {
                throw new RepositoryException("Failed to load node");
            }
        };

        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrNodeFactory.class).build()) {
            nodeModel.get();
            assertLogMessage(interceptor, "Failed to load node with uuid cafebabe-cafe-babe-cafe-babecafebabe", Level.ERROR);
        }
    }

    @Test
    public void testLogMessageOnSet() throws Exception {
        final NodeFactory nodeFactory = JcrNodeFactory.of(root);
        final Model<Node> nodeModel = nodeFactory.getNodeModelByNode(root);
        final Node brokenNode = EasyMock.createMock(Node.class);
        expect(brokenNode.getIdentifier()).andThrow(new RepositoryException("I have no id"));
        expect(brokenNode.getPath()).andReturn("/broken-node");
        EasyMock.replay(brokenNode);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrNodeFactory.class).build()) {
            nodeModel.set(brokenNode);
            assertLogMessage(interceptor, "Failed to retrieve uuid from node /broken-node", Level.ERROR);
        }
        EasyMock.verify(brokenNode);
    }

}
