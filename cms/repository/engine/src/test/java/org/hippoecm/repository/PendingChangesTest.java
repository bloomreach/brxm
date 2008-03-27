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

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoNode;

public class PendingChangesTest extends TestCase {
    private final static String SVN_ID = "$Id$";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private HippoSession session;
    private HippoNode root;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = (HippoSession) server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        root = (HippoNode) session.getRootNode().addNode("test");
        session.save();
    }

    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        session.logout();
        server.close();
    }

    public void testSanity() throws Exception {
        NodeIterator changes;
        Node node;
        Set<String> paths = new HashSet<String>();

        assertFalse(session.hasPendingChanges());
        changes = session.pendingChanges();
        assertFalse(changes.hasNext());

        assertFalse(root.isModified());
        changes = root.pendingChanges();
        assertFalse(changes.hasNext());

        root.addNode("test0","nt:unstructured");
        node = root.addNode("test1","nt:unstructured");
        root.addNode("test2","nt:unstructured");
        node.addNode("aap", "hippo:testdocument").addMixin("hippo:harddocument");
        node.addNode("noot", "hippo:testdocument").addMixin("hippo:harddocument");
        node = node.addNode("mies", "hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.addNode("zus", "hippo:document").addMixin("hippo:harddocument");

        assertTrue(session.hasPendingChanges());

        changes = session.pendingChanges();
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

        changes = session.pendingChanges(null, "hippo:document");
        for(paths.clear(); changes.hasNext(); )
            paths.add(changes.nextNode().getPath());
        assertEquals(4, paths.size());
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));
        assertTrue(paths.contains("/test/test1/mies/zus"));

        changes = session.pendingChanges(null, "hippo:testdocument");
        for(paths.clear(); changes.hasNext(); )
            paths.add(changes.nextNode().getPath());
        assertEquals(3, paths.size());
        assertTrue(paths.contains("/test/test1/aap"));
        assertTrue(paths.contains("/test/test1/noot"));
        assertTrue(paths.contains("/test/test1/mies"));

        session.save();

        assertFalse(session.hasPendingChanges());
        changes = session.pendingChanges();
        assertFalse(changes.hasNext());

        root.getNode("test1").addNode("vuur");

        changes = session.pendingChanges();
        for(paths.clear(); changes.hasNext(); ) {
            paths.add(changes.nextNode().getPath());
        }
        assertEquals(2, paths.size());
        assertTrue(paths.contains("/test/test1"));
        assertTrue(paths.contains("/test/test1/vuur"));
    }
}
