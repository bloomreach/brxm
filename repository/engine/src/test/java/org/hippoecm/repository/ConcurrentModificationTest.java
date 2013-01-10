/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * Concurrent modification tests to test for regressions of data corruption due
 * to http://issues.apache.org/jira/browse/JCR-2129.
 */
public class ConcurrentModificationTest extends RepositoryTestCase {

    private static final String testRoot = "/test";
    private Session userSession;
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node testNode = session.getRootNode().addNode("test");
        testNode.addNode("A").addNode("B");
        testNode.addNode("B");
        testNode.addNode("C");
        session.save();
        userSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (userSession != null && userSession.isLive()) {
            userSession.logout();
            userSession = null;
        }
        session.refresh(false);
        getTestNode().remove();
        session.save();
        super.tearDown();
    }
    
    private Node getUserTestNode() throws Exception {
        return (Node) userSession.getRootNode().getNode("test");
    }

    private Node getTestNode() throws Exception {
        return (Node) session.getRootNode().getNode("test");
    }

    public void testReorderWithAdd() throws Exception {
        getTestNode().orderBefore("C", "A");
        getUserTestNode().addNode("D");
        session.save();
        try {
            userSession.save();
            fail("must throw InvalidItemStateException");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    @Test
    public void testAddWithReorder() throws Exception {
        getTestNode().addNode("D");
        getUserTestNode().orderBefore("C", "A");
        session.save();
        try {
            userSession.save();
            fail("must throw InvalidItemStateException");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }
    
    @Test
    public void testAddWithMoveFrom() throws Exception {
        getTestNode().getNode("A").addNode("D");
        userSession.move(testRoot + "/A/B", testRoot + "/C/B");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testAddWithMoveTo() throws Exception {
        getTestNode().getNode("A").addNode("D");
        userSession.move(testRoot + "/C", testRoot + "/A/C");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testRemoveWithMoveFrom() throws Exception {
        Node d = getTestNode().getNode("A").addNode("D");
        session.save();
        d.remove();
        userSession.move(testRoot + "/A/B", testRoot + "/C/B");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testRemoveWithMoveTo() throws Exception {
        Node d = getTestNode().getNode("A").addNode("D");
        session.save();
        d.remove();
        userSession.move(testRoot + "/C", testRoot + "/A/C");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testMoveFromWithAdd() throws Exception {
        session.move(testRoot + "/A/B", testRoot + "/C/B");
        getUserTestNode().getNode("A").addNode("D");
        session.save();
        userSession.save();
    }

    @Test
    public void testMoveToWithAdd() throws Exception {
        session.move(testRoot + "/C", testRoot + "/A/C");
        getUserTestNode().getNode("A").addNode("D");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testMoveFromWithRemove() throws Exception {
        Node d = getUserTestNode().getNode("A").addNode("D");
        userSession.save();
        session.move(testRoot + "/A/B", testRoot + "/C/B");
        d.remove();
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testMoveToWithRemove() throws Exception {
        Node d = getUserTestNode().getNode("A").addNode("D");
        userSession.save();
        session.move(testRoot + "/C", testRoot + "/A/C");
        d.remove();
        getTestNode().getSession().save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testAddAdd() throws Exception {
        getTestNode().getNode("A").addNode("D");
        getUserTestNode().getNode("A").addNode("E");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testRemoveRemove() throws Exception {
        Node d = getTestNode().getNode("A").addNode("D");
        session.save();
        d.remove();
        getUserTestNode().getNode("A").getNode("B").remove();
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    @Test
    public void testMove() throws Exception {
        String srcAbsPath = getTestNode().addNode("A").getPath();
        String destAbsPath1 = getTestNode().addNode("B").getPath() + "/D";
        String destAbsPath2 = getTestNode().addNode("C").getPath() + "/D";
        session.save();
        session.move(srcAbsPath, destAbsPath1);
        userSession.move(srcAbsPath, destAbsPath2);
        session.save();
        try {
            userSession.save();
            fail("InvalidItemStateException expected");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    @Test
    public void testAdd() throws Exception {
        Node n = getTestNode().addNode("D");
        session.save();
        n.setProperty("pA", "foo");
        getUserTestNode().getNode("D").setProperty("pB", "bar");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    @Ignore
    public void testAddSameName() throws Exception {
        Node n = getTestNode().addNode("D");
        session.save();
        n.setProperty("pA", "foo");
        getUserTestNode().getNode("D").setProperty("pA", "bar");
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
        assertEquals("bar", n.getProperty("pA").getString());
    }

    @Test
    public void testRemove() throws Exception {
        Node n = getTestNode().addNode("D");
        n.setProperty("pA", "foo");
        n.setProperty("pB", "bar");
        session.save();
        n.getProperty("pA").remove();
        getUserTestNode().getNode("D").getProperty("pB").remove();
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    @Ignore
    public void testRemoveSameName() throws Exception {
        Node n = getTestNode().addNode("D");
        n.setProperty("pA", "foo");
        session.save();
        n.getProperty("pA").remove();
        getUserTestNode().getNode("D").getProperty("pA").remove();
        session.save();
        try {
            userSession.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }
}

