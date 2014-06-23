/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MockSessionTest {

    @Test
    public void testGetNodeRootPath() throws RepositoryException {
        MockSession session = new MockSession(MockNode.root());
        assertNotNull(session.getNode("/"));
    }

    @Test
    public void testNodeExistsRootPath() throws RepositoryException {
        MockSession session = new MockSession(MockNode.root());
        assertTrue(session.nodeExists("/"));
    }

    @Test(expected = PathNotFoundException.class)
    public void testGetItemNonExistent() throws RepositoryException {
        MockSession session = new MockSession(MockNode.root());
        session.getItem("/foo/bar");
    }

    @Test
    public void testGetNodeDescendant() throws RepositoryException {
        MockSession session = new MockSession(createRootFooBarMockNode());
        assertTrue(session.nodeExists("/foo/bar"));
    }

    @Test
    public void testNodeExistsDescendant() throws RepositoryException {
        MockSession session = new MockSession(createRootFooBarMockNode());
        assertNotNull(session.getNode("/foo/bar"));
    }

    @Test(expected = ItemNotFoundException.class)
    public void getUnknownNodeByIdentifier() throws RepositoryException {
        new MockSession(createRootFooBarMockNode()).getNodeByIdentifier("no-such-node");
    }

    @Test
    public void getKnownNodeByIdentifier() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode child = new MockNode("child");
        root.addNode(child);
        MockNode grandchild = new MockNode("grandchild");
        child.addNode(grandchild);

        MockSession session = new MockSession(root);
        assertSame(root, session.getNodeByIdentifier(root.getIdentifier()));
        assertSame(child, session.getNodeByIdentifier(child.getIdentifier()));
        assertSame(grandchild, session.getNodeByIdentifier(grandchild.getIdentifier()));
    }

    @Test
    public void getUserIdReturnsEmptyString() throws RepositoryException {
        Session session = MockNode.root().getSession();
        assertEquals("", session.getUserID());
    }

    @Test
    public void valueFactoryCanCreateBinaries() throws RepositoryException, IOException {
        Session session = new MockSession(MockNode.root());
        ValueFactory factory = session.getValueFactory();

        byte[] data = new byte[10];
        data[3] = 42;
        Binary binary = factory.createBinary(new ByteArrayInputStream(data));
        assertEquals(data.length, binary.getSize());
        IOUtils.contentEquals(new ByteArrayInputStream(data), binary.getStream());
    }

    private MockNode createRootFooBarMockNode() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode foo = new MockNode("foo");
        MockNode bar = new MockNode("bar");
        foo.addNode(bar);
        root.addNode(foo);
        return root;
    }

}
