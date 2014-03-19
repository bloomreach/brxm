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
package org.hippoecm.repository.decorating;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MirrorTest extends RepositoryTestCase {

    private static String[] contents1 = new String[] {
        "/test", "nt:unstructured",
        "/test/documents", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "niet", "hier",
        "/test/documents/test1", "nt:unstructured",
        "/test/documents/test2", "nt:unstructured",
        "wel","anders",
        "/test/documents/test3", "nt:unstructured",
        "/test/documents/test3/test4", "nt:unstructured",
        "lachen", "zucht",
        "/test/documents/test3/test4/test5", "nt:unstructured",
        "/test/documents/test5", "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/documents/test5/test5", "hippo:document",
        "jcr:mixinTypes", "mix:versionable",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured",
        "/test/documents/testmore", "nt:unstructured"
    };

    private static String[] contents2 = new String[] {
        "/test/navigation", "nt:unstructured",
        "/test/navigation/mirror", "hippo:mirror",
        "hippo:docbase", "/test/documents",
        "/test/navigation/subtypemirror", "hippo:subtypemirror",
        "jcr:mixinTypes", "mix:referenceable",
        "hippo:docbase", "/test/documents"
    };
    
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(contents1, session);
        session.save();
        build(contents2, session);;
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSoftReference() throws Exception {
        assertTrue(session.getRootNode().hasNode("test/navigation/mirror/test5"));
        Node n = session.getRootNode().getNode("test/navigation/mirror/test5");
        assertTrue(n.hasNode(n.getName()));
        assertTrue(n.isNodeType("hippo:handle"));
        assertTrue(n.getNode(n.getName()).isNodeType("hippo:document"));
        assertTrue(n.isNodeType("hipposys:softhandle"));
        assertTrue(n.getNode(n.getName()).isNodeType("hipposys:softdocument"));
        assertTrue(n.hasProperty("hippo:uuid"));
        assertTrue(n.getNode(n.getName()).hasProperty("hippo:uuid"));
        assertNotNull(n.getProperty("hippo:uuid"));
        assertNotNull(n.getNode(n.getName()).getProperty("hippo:uuid"));
    }

    /**
     * This tests whether when creating new sessions, each accessing a
     * virtual tree and then logging out, all memory is released.  Due
     * to an issue when upgrading to jackrabbit 2.2.9 this was not the
     * case.  This test by default does not go out of memory, you
     * either need to limit the maximum amount of memory to 32Mb, or
     * increase the iteration count a lot.  This means that this unit
     * test is better suited for an integration or long running test suite.
     */
    @Test
    public void testREPO246() throws Exception {
        for(int i=0; i<2500; i++) {
            Session newSession;
            newSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            newSession.getRootNode().getNode("test/navigation").getNode("mirror").getNode("test1");
            newSession.logout();
        }
    }

    @Test
    public void testMirror() throws Exception {
        assertNotNull(session.getRootNode());
        assertTrue(session.getRootNode().hasNode("test/navigation"));
        assertNotNull(session.getRootNode().getNode("test/navigation"));
        assertTrue(session.getRootNode().getNode("test/navigation").hasNode("mirror"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("mirror"));
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("mirror").hasProperty("hippo:docbase"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("mirror").getProperty("hippo:docbase"));
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("mirror").hasNode("test1"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("mirror").getNode("test1"));

        session.getRootNode().getNode("test").addNode("dummy");
        session.getRootNode().getNode("test/documents").addNode("test-a","nt:unstructured").setProperty("test-b","test-c");
        session.getRootNode().getNode("test/documents").getNode("test1").addNode("test-x");
        session.save();
        session.refresh(true);

        assertTrue(session.getRootNode().getNode("test/navigation").getNode("mirror").hasNode("test-a"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("mirror").getNode("test-a"));
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("mirror").getNode("test1").hasNode("test-x"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("mirror").getNode("test1").getNode("test-x"));
        assertFalse(session.getRootNode().getNode("test/navigation").getNode("mirror").hasNode("test1[2]"));
    }
    
    @Test
    public void testSubTypeMirror() throws Exception {        
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").hasProperty("hippo:docbase"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").getProperty("hippo:docbase"));
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").hasNode("test1"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").getNode("test1"));

        session.getRootNode().getNode("test").addNode("dummy");
        session.getRootNode().getNode("test/documents").addNode("test-a","nt:unstructured").setProperty("test-b","test-c");
        session.getRootNode().getNode("test/documents").getNode("test1").addNode("test-x");
        session.save();
        session.refresh(true);

        assertTrue(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").hasNode("test-a"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").getNode("test-a"));
        assertTrue(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").getNode("test1").hasNode("test-x"));
        assertNotNull(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").getNode("test1").getNode("test-x"));
        assertFalse(session.getRootNode().getNode("test/navigation").getNode("subtypemirror").hasNode("test1[2]"));
    }
}
