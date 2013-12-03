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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PendingChangesTest extends RepositoryTestCase {

    private HippoNode test;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        test = (HippoNode) session.getRootNode().addNode("test");
        session.save();
    }

    @Test
    public void testSanity() throws Exception {
        NodeIterator changes;
        Node node;
        Set<String> paths = new HashSet<String>();

        assertFalse(session.hasPendingChanges());
        changes = ((HippoSession)session).pendingChanges();
        assertFalse(changes.hasNext());

        assertFalse(test.isModified());
        changes = test.pendingChanges();
        assertFalse(changes.hasNext());

        test.setProperty("prop", "test");
        assertTrue(test.isModified());
        changes = test.pendingChanges();
        assertTrue(changes.hasNext());
        changes = ((HippoSession)session).pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(1, paths.size());
        assertTrue(paths.contains("/test"));

        session.save();

        test.addNode("test0","nt:unstructured");
        node = test.addNode("test1","nt:unstructured");
        test.addNode("test2", "nt:unstructured");
        node.addNode("aap", "hippo:testdocument").addMixin("mix:versionable");
        node.addNode("noot", "hippo:testdocument").addMixin("mix:versionable");
        node = node.addNode("mies", "hippo:testdocument");
        node.addMixin("mix:versionable");
        node.addNode("zus", "hippo:document").addMixin("mix:versionable");

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

        changes = test.pendingChanges();
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

        test.getNode("test1").addNode("vuur");

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
        Node node = test.addNode("test", "nt:unstructured");
        node.remove();

        NodeIterator changes = ((HippoSession) session).pendingChanges(null, null, true);
        Set<String> paths = new HashSet<String>();
        for (paths.clear(); changes.hasNext();) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(1, paths.size());
    }

    @Test
    public void testMoveNode() throws Exception {
        final Node foo = test.addNode("foo");
        session.save();
        session.move(foo.getPath(), test.getPath() + "/bar");
        final NodeIterator iterator = ((HippoSession) session).pendingChanges();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals("Unexepected number of pending changes after move", 2, count);
    }

}
