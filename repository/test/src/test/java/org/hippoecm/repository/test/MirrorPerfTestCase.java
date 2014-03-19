/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class MirrorPerfTestCase extends RepositoryTestCase {
    private String[] content = {
        "/test", "nt:unstructured",
        "/test/root", "hippostd:folder",
        "jcr:mixinTypes", "mix:versionable",
        "/test/mirror", "hippo:mirror",
        "hippo:docbase", "/test/root"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node root = session.getRootNode();
        if (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        session.save();
        build(content, session);
        // for a proper test use at least the following settings:
        // build(session.getRootNode().getNode("test/root"), 3, 4, 5, 15);
        build(session.getRootNode().getNode("test/root"), 3, 1, 1, 1);
    }
    
    private int build(Node node, int depth, int fanout, int docsPerNode, int docsPerLeaf) throws RepositoryException {
        int total = 0;
        if(depth > 0) {
            for(int i=0; i<fanout; i++) {
                Node child = node.addNode("folder"+i, "hippostd:folder");
                child.addMixin("mix:versionable");
                total += build(child, depth-1, fanout, docsPerNode, docsPerLeaf);
            }
            for(int i=0; i<docsPerNode; i++) {
                Node child = node.addNode("document"+i, "hippo:handle");
                child.addMixin("hippo:hardhandle");
                child = child.addNode("document"+i, "hippo:testdocument");
                child.addMixin("mix:versionable");
                ++total;
            }
        } else {
            for(int i=0; i<docsPerLeaf; i++) {
                Node child = node.addNode("document"+i, "hippo:handle");
                child.addMixin("hippo:hardhandle");
                child = child.addNode("document"+i, "hippo:testdocument");
                child.addMixin("mix:versionable");
                ++total;
            }
        }
        node.getSession().save();
        return total;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.refresh(false);
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
            }
        }
        super.tearDown();
    }

    private void traverse(Node node) throws RepositoryException {
        for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            Node child = iter.nextNode();
            if(child != null) {
                traverse(child);
            }
        }
    }
    
    @Test
    public void testTraverseBase() throws Exception {
        Session testSession = server.login("admin", "admin".toCharArray());
        long tAfter, tBefore = System.currentTimeMillis();
        traverse(testSession.getRootNode().getNode("test/root"));
        tAfter = System.currentTimeMillis();
        long duration = tAfter - tBefore;
        System.out.println("traversal "+Double.toString(duration) + "ms");
    }

    @Test
    public void testTraverseMirror() throws Exception {
        long duration = testTraverse();        
        System.out.println("traversal " + Double.toString(duration) + "ms");
    }


    @Test
    public void testTraverseConcurrentMirror() throws Exception {
        long duration = testConcurrent();        
        System.out.println("traversal " + Double.toString(duration) + "ms");
    }

    private long testTraverse() throws RepositoryException {
        Session testSession = server.login("admin", "admin".toCharArray());
        traverse(testSession.getRootNode().getNode("test/mirror"));
        long tAfter, tBefore = System.currentTimeMillis();
        for(int i=0; i<10; i++) {
            testSession.refresh(false);
            traverse(testSession.getRootNode().getNode("test/mirror"));
        }
        tAfter = System.currentTimeMillis();
        testSession.logout();
        long duration = tAfter - tBefore;
        return duration;
    }
    
    private long testConcurrent() throws RepositoryException, InterruptedException {
        Thread[] threads = new Thread[20];
        for(int i=0; i<threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        testTraverse();
                    } catch(RepositoryException ex) {
                    }
                }
            });
        }
        long tAfter, tBefore = System.currentTimeMillis();
        for(int i=0; i<threads.length; i++)
            threads[i].start();
        for(int i=0; i<threads.length; i++)
            threads[i].join();
        tAfter = System.currentTimeMillis();
        long duration = tAfter - tBefore;
        return duration;
    }
}
