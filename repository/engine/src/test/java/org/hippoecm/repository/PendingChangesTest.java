/*
 *  Copyright 2008 Hippo.
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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PendingChangesTest extends RepositoryTestCase {

    private HippoNode root;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        root = (HippoNode) session.getRootNode().addNode("test");
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        //session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void testSanity() throws Exception {
        NodeIterator changes;
        Node node;
        Set<String> paths = new HashSet<String>();

        assertFalse(session.hasPendingChanges());
        changes = ((HippoSession)session).pendingChanges();
        assertFalse(changes.hasNext());

        assertFalse(root.isModified());
        changes = root.pendingChanges();
        assertFalse(changes.hasNext());

        root.setProperty("prop", "test");
        assertTrue(root.isModified());
        changes = root.pendingChanges();
        assertTrue(changes.hasNext());
        changes = ((HippoSession)session).pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(1, paths.size());
        assertTrue(paths.contains("/test"));

        session.save();

        root.addNode("test0","nt:unstructured");
        node = root.addNode("test1","nt:unstructured");
        root.addNode("test2","nt:unstructured");
        node.addNode("aap", "hippo:testdocument").addMixin("hippo:harddocument");
        node.addNode("noot", "hippo:testdocument").addMixin("hippo:harddocument");
        node = node.addNode("mies", "hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.addNode("zus", "hippo:document").addMixin("hippo:harddocument");

        assertTrue(session.hasPendingChanges());

        changes = ((HippoSession)session).pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(8, paths.size());
        assertTrue(paths.contains("/test"));
        assertTrue(paths.contains("/test/test0"));
        assertTrue(paths.contains("/test/test1"));
        assertTrue(paths.contains("/test/test2"));
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));
        assertTrue(paths.contains("/test/test1/mies/zus"));

        changes = root.pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(7, paths.size());
        assertTrue(paths.contains("/test/test0"));
        assertTrue(paths.contains("/test/test1"));
        assertTrue(paths.contains("/test/test2"));
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));
        assertTrue(paths.contains("/test/test1/mies/zus"));

        changes = ((HippoSession)session).pendingChanges(null, "hippo:document");
        for(paths.clear(); changes.hasNext(); )
            paths.add(changes.nextNode().getPath());
        assertEquals(4, paths.size());
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));
        assertTrue(paths.contains("/test/test1/mies/zus"));

        changes = ((HippoSession)session).pendingChanges(null, "hippo:testdocument");
        for(paths.clear(); changes.hasNext(); )
            paths.add(changes.nextNode().getPath());
        assertEquals(3, paths.size());
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));

        session.save();

        assertFalse(session.hasPendingChanges());
        changes = ((HippoSession)session).pendingChanges();
        assertFalse(changes.hasNext());

        root.getNode("test1").addNode("vuur");

        changes = ((HippoSession)session).pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(2, paths.size());
        assertTrue(paths.contains("/test/test1"));
        assertTrue(paths.contains("/test/test1/vuur"));
    }

    @Test
    public void testNodeFilter() throws Exception {
        Node node = root.addNode("test", "nt:unstructured");
        node.remove();

        NodeIterator changes = ((HippoSession) session).pendingChanges(null, null, true);
        Set<String> paths = new HashSet<String>();
        for (paths.clear(); changes.hasNext();) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(1, paths.size());
    }

    @Test
    public void testThresholdExceeded() throws Exception {
        if (external != null) {
            return; // not a valid test for remote repositories
        }
        assertFalse(server.stateThresholdExceeded(session, null));
        int count = 0;
        for (count = 0; count < 100; count++) {
            Node node = root.addNode("n" + count, "nt:unstructured");
            /*for (int i = 0; i < 25; i++) {
                Node child = node.addNode("c" + i, "nt:unstructured");
                for (int p = 0; p < 25; p++) {
                    child.setProperty("p" + p, "");
                }
            }*/
            if (server.stateThresholdExceeded(session, null))
                break;
        }
        assertTrue(count > 2);
        assertTrue(server.stateThresholdExceeded(session, null));
        session.refresh(false);
        assertFalse(server.stateThresholdExceeded(session, null));
    }
}
