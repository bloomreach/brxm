/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.api.ISO9075Helper;

public class TrivialServerTest extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;
    private Node root;

    public void setUp() throws RepositoryException {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        root = session.getRootNode();
    }

    public void tearDown() throws Exception {
        session.logout();
        server.close();
    }

    public void testTrivialNodeOperations()  {
        try {
            root.addNode("x");
        } catch (RepositoryException e) {
            fail("Failed to add node: " + e.getMessage());
            e.printStackTrace();
        }

        // transient
        try {
            assertNotNull(root.getNode("x"));
        } catch (RepositoryException e) {
            fail("Failed to find node: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("Failed to save node: " + e.getMessage());
            e.printStackTrace();
        }

        // after persist
        try {
            assertNotNull(root.getNode("x"));
        } catch (RepositoryException e) {
            fail("Failed to find node: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            root.getNode("x").remove();
        } catch (RepositoryException e) {
            fail("Failed to delete node: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("Failed to save node deletion: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            root.getNode("x");
            fail("Deleted node found.");
        } catch (PathNotFoundException e) {
            // ok
        } catch (RepositoryException e) {
            fail("Failed to not find node: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void testEncodedNode() throws RepositoryException {
        String name = "2..,!@#$%^&*()_-[]{}|\\:;'\".,/?testnode";
        Node root = session.getRootNode();
        Node encodedNode = root.addNode(ISO9075Helper.encodeLocalName(name));
        assertNotNull(encodedNode);
        assertEquals(ISO9075Helper.encodeLocalName(name),encodedNode.getName());
        session.save();
        Node encodedNode2 = root.getNode(ISO9075Helper.encodeLocalName(name));
        assertEquals(encodedNode, encodedNode2);
        assertEquals(ISO9075Helper.encodeLocalName(name),encodedNode.getName());
        encodedNode2.remove();
    }

}
